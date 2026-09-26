import {build} from "esbuild";

await build({
  entryPoints: [
    "playground/playground.js",
    "playground/playground-worker.js",
  ],
  bundle: true,
  minify: true,
  outdir: "src/assets/playground",
  platform: "browser",
  target: "es2022",
});
