<?php

namespace YAMLScript;

use FFI;
use RuntimeException;

class YAMLScript
{
    private static ?FFI $ffi = null;
    private static string $libPath;
    private $isolateThread = null;

    public function __construct(string $libPath = null)
    {
        if ($libPath !== null) {
            self::$libPath = $libPath;
        } elseif (!isset(self::$libPath)) {
            // Default library path based on common locations
            // Determine platform and library file name

            // Get version from composer.json
            $composerJson = json_decode(
                file_get_contents(__DIR__ . '/../composer.json'),
                true
            );
            $version = $composerJson['version'];

            if (PHP_OS_FAMILY === 'Windows') {
                $libName = 'libys.dll';
            } else {
                $so = PHP_OS === 'Darwin' ? 'dylib' : 'so';
                // Build library name with version
                $libName = "libys.$so.$version";
            }

            // Check library path environment variables
            $paths = [];
            if (PHP_OS_FAMILY === 'Windows') {
                // The dll is found via the PATH directories
                $envPath = getenv('PATH');
                if ($envPath) {
                    foreach (explode(PATH_SEPARATOR, $envPath) as $path) {
                        $paths[] = "$path/$libName";
                    }
                }
                $home = getenv('USERPROFILE') ?: getenv('HOME');
            } else {
                if (PHP_OS === 'Darwin') {
                    $dyldLibraryPath = getenv('DYLD_LIBRARY_PATH');
                    if ($dyldLibraryPath) {
                        foreach (explode(':', $dyldLibraryPath) as $path) {
                            $paths[] = "$path/$libName";
                        }
                    }
                }
                $ldLibraryPath = getenv('LD_LIBRARY_PATH');
                if ($ldLibraryPath) {
                    foreach (explode(':', $ldLibraryPath) as $path) {
                        $paths[] = "$path/$libName";
                    }
                }

                // Add default locations
                $paths[] = "/usr/local/lib/$libName";
                $home = getenv('HOME');
            }
            $paths[] = $home . "/.local/lib/$libName";
            foreach ($paths as $path) {
                if (file_exists($path)) {
                    self::$libPath = $path;
                    break;
                }
            }
            if (!isset(self::$libPath)) {
                throw new RuntimeException(
                    'libys.so not found. Please specify the path manually.'
                );
            }
        }

        if (self::$ffi === null) {
            self::$ffi = FFI::cdef('
                int graal_create_isolate(
                    void* params, void** isolate, void** thread
                );
                int graal_tear_down_isolate(void* thread);
                char* load_ys_to_json(void* thread, const char* input);
            ', self::$libPath);
        }

        // Create isolate thread. The out-parameter CData must be
        // kept in variables: taking FFI::addr() of a temporary
        // leaves a dangling pointer that graal_create_isolate then
        // writes through (heap corruption, config-dependent crash).
        $isolate = self::$ffi->new('void*');
        $thread = self::$ffi->new('void*');
        $result = self::$ffi->graal_create_isolate(
            null,
            FFI::addr($isolate),
            FFI::addr($thread)
        );

        if ($result !== 0) {
            throw new RuntimeException('Failed to create isolate');
        }

        $this->isolateThread = $thread;
    }

    public function __destruct()
    {
        if ($this->isolateThread !== null) {
            self::$ffi->graal_tear_down_isolate($this->isolateThread);
            $this->isolateThread = null;
        }
    }

    public function compile(string $input): string
    {
        if (!self::$ffi) {
            throw new RuntimeException('FFI not initialized');
        }

        if ($this->isolateThread === null) {
            throw new RuntimeException('No active isolate thread');
        }

        try {
            if (empty($input)) {
                return json_encode([]);
            }

            $result = self::$ffi->load_ys_to_json(
                $this->isolateThread,
                $input
            );

            if ($result === null) {
                throw new RuntimeException('Compilation failed');
            }

            $output = FFI::string($result);
            $response = json_decode($output, true);

            if (isset($response['error'])) {
                throw new RuntimeException($response['error']['cause']);
            }

            if (!isset($response['data'])) {
                throw new RuntimeException('Unexpected response from libys');
            }

            return json_encode($response['data']);
        } catch (FFI\Exception $e) {
            throw new RuntimeException('FFI error: ' . $e->getMessage());
        }
    }
}
