# frozen_string_literal: true

# Copyright 2023-2024 Ingy dot Net
# This code is licensed under MIT license (See License for details)

require 'fiddle'
require 'fiddle/import'
require 'json'

require_relative 'yamlscript/version'

# Ruby binding for the libyamlscript shared library.
class YAMLScript
  Error = Class.new(StandardError)

  # TODO: This value is automatically updated by 'make bump'.
  # The version number is used to find the correct shared library file.
  # We currently only support binding to an exact version of libyamlscript.
  YAML_SCRIPT_VERSION = '0.1.34'

  # A low-level interface to the native library
  module LibYAMLScript
    extend Fiddle::Importer

    def self.extension
      case RUBY_PLATFORM
      when /darwin/
        'dylib'
      when /linux/
        'so'
      else
        raise Error, "Unsupported platform #{RUBY_PLATFORM} for yamlscript."
      end
    end

    def self.filename
      "libyamlscript.#{extension}.#{YAML_SCRIPT_VERSION}"
    end

    # Returns an array of library paths extracted from the LD_LIBRARY_PATH environment variable.
    # If the environment variable is not set will return an array with `/usr/local/lib` only.
    def self.ld_library_paths
      env_value = ENV.fetch('LD_LIBRARY_PATH', '')
      paths = env_value.split(':')
      paths << '/usr/local/lib'
    end

    # Find the libyamlscript shared library file path
    def self.path
      name = filename
      path = ld_library_paths.map { |dir| File.join(dir, name) }.detect { |file| File.exist?(file) }

      raise Error, "Shared library file `#{name}` not found" unless path

      path
    end

    dlload path

    extern 'int graal_create_isolate(void* params, void** isolate, void** thread)'
    extern 'int graal_tear_down_isolate(void* thread)'
    extern 'char* load_ys_to_json(void* thread, char* yamlscript)'
  end

  # Interface with the libyamlscript shared library.
  #
  # @example
  #   require 'yamlscript'
  #
  #   YAMLScript.load(IO.read('file.ys'))
  def self.load(ys_code, **options)
    new(**options).load(ys_code)
  end

  attr_reader :options, :error

  def initialize(**options)
    # config not used yet
    @options = options

    # Create a new GraalVM isolate for life of the YAMLScript instance
    @isolate = Fiddle::Pointer.malloc(Fiddle::SIZEOF_VOIDP)
    @error = nil
  end

  # Compile and eval a YAMLScript string and return the result
  def load(ys_code)
    # Create a new GraalVM isolate thread for each call to load()
    thread = Fiddle::Pointer.malloc(Fiddle::SIZEOF_VOIDP)
    LibYAMLScript.graal_create_isolate(nil, @isolate.ref, thread.ref)

    # Call 'load_ys_to_json' function in libyamlscript shared library
    json_data = LibYAMLScript.load_ys_to_json(thread, ys_code)
    resp = JSON.parse(json_data.to_s)
    raise Error, 'Failed to tear down isolate' unless LibYAMLScript.graal_tear_down_isolate(thread).zero?
    raise Error, @error['cause'] if (@error = resp['error'])
    data = resp.fetch('data') do
      raise Error, 'Unexpected response from libyamlscript'
    end

    data
  end
end
