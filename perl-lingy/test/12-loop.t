use YAMLScript::Test;

test_ys_to_ly <<'...';
- - |
    loop [x 1]:
    - if (x < 5):
      - - prn(x)
        - recur: inc(x)
      - prn: "done"
  - (loop [x 1] (if (< x 5) (do (prn x) (recur (inc x))) (prn "done")))
...
