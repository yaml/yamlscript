const ROOT = "/playground";
const encoder = new TextEncoder();

function fsError(code, message, path) {
  const error = new Error(`${code}: ${message}${path ? `, '${path}'` : ""}`);
  error.code = code;
  error.path = path;
  return error;
}

function normalize(path) {
  const input = String(path || ".");
  const absolute = input.startsWith("/") ? input : `${ROOT}/${input}`;
  const parts = [];
  for (const part of absolute.split("/")) {
    if (!part || part === ".") continue;
    if (part === "..") {
      parts.pop();
    } else {
      parts.push(part);
    }
  }
  return `/${parts.join("/")}`;
}

function stats(node) {
  const directory = node.type === "directory";
  const size = directory ? 0 : node.data.length;
  return {
    dev: 0,
    ino: node.ino,
    mode: directory ? 0o40555 : 0o100444,
    nlink: 1,
    uid: 0,
    gid: 0,
    rdev: 0,
    size,
    blksize: 4096,
    blocks: Math.ceil(size / 512),
    atimeMs: 0,
    mtimeMs: 0,
    ctimeMs: 0,
    isDirectory: () => directory,
    isFile: () => !directory,
    isSymbolicLink: () => false,
  };
}

export function createVirtualFileSystem(output) {
  const nodes = new Map();
  const descriptors = new Map();
  let nextIno = 1;
  let nextFd = 10;

  function addDirectory(path) {
    if (!nodes.has(path)) {
      nodes.set(path, {type: "directory", ino: nextIno++});
    }
  }

  function getNode(path) {
    const resolved = normalize(path);
    const node = nodes.get(resolved);
    if (!node) throw fsError("ENOENT", "no such file or directory", path);
    return {node, resolved};
  }

  function readonly(path) {
    return fsError("EROFS", "read-only file system", path);
  }

  function callbackError(callback, action) {
    try {
      action();
    } catch (error) {
      callback(error);
    }
  }

  const api = {
    constants: {
      O_RDONLY: 0,
      O_WRONLY: 1,
      O_RDWR: 2,
      O_CREAT: 64,
      O_EXCL: 128,
      O_TRUNC: 512,
      O_APPEND: 1024,
      O_DIRECTORY: 65536,
    },

    mount(files) {
      nodes.clear();
      descriptors.clear();
      nextIno = 1;
      nextFd = 10;
      addDirectory("/");
      addDirectory(ROOT);
      for (const [relative, source] of Object.entries(files)) {
        if (!relative || relative.startsWith("/") || relative.includes("\\")) {
          throw fsError("EINVAL", "invalid project path", relative);
        }
        const parts = relative.split("/");
        if (parts.some((part) => !part || part === "." || part === "..")) {
          throw fsError("EINVAL", "invalid project path", relative);
        }
        let directory = ROOT;
        for (const part of parts.slice(0, -1)) {
          directory = `${directory}/${part}`;
          addDirectory(directory);
        }
        const path = `${ROOT}/${relative}`;
        nodes.set(path, {
          type: "file",
          data: encoder.encode(String(source)),
          ino: nextIno++,
        });
      }
    },

    writeSync(fd, buffer) {
      if (fd === 1 || fd === 2) return output.writeSync(fd, buffer);
      throw readonly();
    },

    write(fd, buffer, offset, length, position, callback) {
      if (fd !== 1 && fd !== 2) {
        callback(readonly());
        return;
      }
      output.write(fd, buffer, offset, length, position, callback);
    },

    open(path, flags, _mode, callback) {
      callbackError(callback, () => {
        if (flags !== 0 && flags !== "r") throw readonly(path);
        const {node} = getNode(path);
        if (node.type !== "file") {
          throw fsError("EISDIR", "illegal operation on a directory", path);
        }
        const fd = nextFd++;
        descriptors.set(fd, {node, position: 0});
        callback(null, fd);
      });
    },

    read(fd, buffer, offset, length, position, callback) {
      callbackError(callback, () => {
        const descriptor = descriptors.get(fd);
        if (!descriptor) throw fsError("EBADF", "bad file descriptor");
        const start = position === null ? descriptor.position : position;
        const data = descriptor.node.data.subarray(start, start + length);
        buffer.set(data, offset);
        if (position === null) descriptor.position += data.length;
        callback(null, data.length);
      });
    },

    close(fd, callback) {
      if (!descriptors.delete(fd)) {
        callback(fsError("EBADF", "bad file descriptor"));
      } else {
        callback(null);
      }
    },

    stat(path, callback) {
      callbackError(callback, () => callback(null, stats(getNode(path).node)));
    },

    lstat(path, callback) {
      api.stat(path, callback);
    },

    fstat(fd, callback) {
      const descriptor = descriptors.get(fd);
      if (!descriptor) {
        callback(fsError("EBADF", "bad file descriptor"));
      } else {
        callback(null, stats(descriptor.node));
      }
    },

    readdir(path, callback) {
      callbackError(callback, () => {
        const {node, resolved} = getNode(path);
        if (node.type !== "directory") {
          throw fsError("ENOTDIR", "not a directory", path);
        }
        const prefix = resolved === "/" ? "/" : `${resolved}/`;
        const entries = new Set();
        for (const candidate of nodes.keys()) {
          if (candidate.startsWith(prefix)) {
            const name = candidate.slice(prefix.length).split("/", 1)[0];
            if (name) entries.add(name);
          }
        }
        callback(null, [...entries]);
      });
    },

    fsync(_fd, callback) {
      callback(null);
    },

    readlink(path, callback) {
      callback(fsError("EINVAL", "invalid argument", path));
    },
  };

  for (const name of [
    "chmod",
    "chown",
    "fchmod",
    "fchown",
    "ftruncate",
    "lchown",
    "link",
    "mkdir",
    "rename",
    "rmdir",
    "symlink",
    "truncate",
    "unlink",
    "utimes",
  ]) {
    api[name] = (...args) => args.at(-1)(readonly(args[0]));
  }

  api.mount({});
  return api;
}

export function installVirtualFileSystem(runtimeFs) {
  const output = {
    writeSync: runtimeFs.writeSync.bind(runtimeFs),
    write: runtimeFs.write.bind(runtimeFs),
  };
  const virtualFs = createVirtualFileSystem(output);
  Object.assign(runtimeFs, virtualFs);
  return runtimeFs;
}
