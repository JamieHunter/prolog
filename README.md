# Prolog
This is an implementation of Prolog written in Java. At this time, I'm focused on getting it functioning close to Standard Prolog. It is mostly there, and should be able to run many prolog programs. Future work:

## Better error handling
The interpreter occasionally bails out, instead of handling errors cleanly
## Better error reporting during consult
Consult should report line numbers / column numbers of errors
## IO Improvements
There's still a lot of cleanup required for the IO
## Optimization
No effort has been done yet to optimize the code, nor compile this to native Java byte code.
