# trees

A catchall utility library for tree related data structures

## Usage


Under construction.

```clojure
(def m (tree-update-in (create-root) [:ts (System/currentTimeMillis) :a 1 :b 2] safe-inc ))
```

## Benchmark

### Insert 10 000 random records with 3 labels i.e a depth of 6.

```
WARNING: Final GC required 1.279175062775832 % of runtime
Goal:  Insert into labeled trie
-----
Case:  :labeled-insert
Evaluation count : 2040 in 60 samples of 34 calls.
             Execution time mean : 29.977902 ms
    Execution time std-deviation : 115.610724 µs
   Execution time lower quantile : 29.825175 ms ( 2.5%)
   Execution time upper quantile : 30.254506 ms (97.5%)
                   Overhead used : 1.758019 ns
```

## License

Copyright © 2014 Gerrit Jansen van Vuuren

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
