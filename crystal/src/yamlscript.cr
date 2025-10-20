require "json"

require "./yamlscript/version"

# Crystal binding for the libys shared library.
class YAMLScript
  class Error < Exception; end

  # This value is automatically updated by 'make bump'.
  # The version number is used to find the correct shared library file.
  # We currently only support binding to an exact version of libys.
  YAMLSCRIPT_VERSION = "0.2.7"

  # A low-level interface to the native library
  module LibYS
    def self.extension
      {% if flag?(:darwin) %}
        "dylib"
      {% elsif flag?(:linux) %}
        "so"
      {% else %}
        raise Error.new("Unsupported platform for yamlscript.")
      {% end %}
    end

    def self.libys_name
      "libys.#{extension}.#{YAMLScript::YAMLSCRIPT_VERSION}"
    end

    # Returns an array of library paths extracted from the LD_LIBRARY_PATH
    # environment variable plus standard system paths
    def self.ld_library_paths
      paths = [] of String

      # Check LD_LIBRARY_PATH first
      if env_path = ENV["LD_LIBRARY_PATH"]?
        paths.concat(env_path.split(":"))
      end

      # Add standard system paths
      paths << "/usr/local/lib"
      paths << "/usr/lib"

      # Add user's local lib directory if HOME is set
      if home = ENV["HOME"]?
        paths << File.join(home, ".local", "lib")
      end

      paths
    end

    # Find the libys shared library file path
    def self.find_libys_path
      name = libys_name
      path = ld_library_paths.find do |dir|
        next if dir.empty?
        file = File.join(dir, name)
        File.exists?(file) ? file : nil
      end

      unless path
        vers = YAMLScript::YAMLSCRIPT_VERSION
        raise Error.new(<<-ERROR)

Shared library file `#{name}` not found
Try: curl https://yamlscript.org/install | VERSION=#{vers} LIB=1 bash
See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript
ERROR
      end

      path
    end

    # IMPORTANT: For Crystal FFI to work with the YAMLScript library:
    # 1. Make sure libys.so is in a standard library location or in
    # LD_LIBRARY_PATH
    # 2. When running your program, use: LD_LIBRARY_PATH=$HOME/.local/lib
    # crystal run your_program.cr
    # See README.md for detailed instructions.
    @[Link("ys")]
    lib Lib
      fun graal_create_isolate(
        params : Void*,
        isolate : Void**,
        thread : Void**,
      ) : Int32
      fun graal_tear_down_isolate(thread : Void*) : Int32
      fun load_ys_to_json(
        thread : Void*,
        yamlscript : LibC::Char*,
      ) : LibC::Char*
    end
  end

  # Interface with the libys shared library.
  #
  # Example:
  #   ```
  #   require "yamlscript"
  #
  #   YAMLScript.load(File.read("file.ys"))
  #   ```
  def self.load(ys_code : String, **options)
    new(**options).load(ys_code)
  end

  getter options : Hash(Symbol, String)
  getter error : Hash(String, String)?

  def initialize(**options)
    # config not used yet
    @options = {} of Symbol => String
    options.each do |key, value|
      @options[key] = value.to_s
    end
    @error = nil

    # Create a new GraalVM isolate for life of the YAMLScript instance
    @isolate = Pointer(Void).malloc(1)
    @thread = Pointer(Void).malloc(1)
  end

  # Compile and eval a YAMLScript string and return the result
  def load(ys_code : String)
    # Create a new GraalVM isolate thread for each call to load()
    isolate_ptr = pointerof(@isolate)
    thread_ptr = pointerof(@thread)

    if LibYS::Lib.graal_create_isolate(
      nil, isolate_ptr, thread_ptr
    ) != 0
      raise Error.new("Failed to create isolate")
    end

    # Call 'load_ys_to_json' function in libys shared library
    json_ptr = LibYS::Lib.load_ys_to_json(@thread, ys_code.to_unsafe)
    json_data = String.new(json_ptr)
    resp = JSON.parse(json_data).as_h

    if LibYS::Lib.graal_tear_down_isolate(@thread) != 0
      raise Error.new("Failed to tear down isolate")
    end

    if error_data = resp["error"]?
      @error = error_data.as_h.transform_values(&.as_s)
      raise Error.new(@error.not_nil!["cause"])
    end

    unless data = resp["data"]?
      raise Error.new("Unexpected response from 'libys'")
    end

    data
  end
end
