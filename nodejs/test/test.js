let YS = require("yamlscript");

let ys = new YS();

let data1 = ys.load("foo: 6 * 7");

console.log(data1);

let data2 = ys.load("!yamlscript/v0/\nfoo:: 6 * 7");

console.log(data2);
