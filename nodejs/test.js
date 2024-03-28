let YS = require("yamlscript");

let ys = new YS();

ys.hello();

let num = ys.triple(7);

console.log(num);

let data = ys.load("foo: bar");

console.log(data);
