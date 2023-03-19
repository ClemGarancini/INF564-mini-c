**Mini-c compilator project - INF564**

*Victor Barberteguy - Clement Garancini

In this project we had to implement a compilator of mini-C, a simplified version of C, in Java. 

**Usage

In order to test our implementation, one can place himself in the folder were this project was download and run the command ./mini-c --option filename.c
where option can be among --parse-only, --type-only, --interp-rtl, --interp-ertl and --debug and filename.c the file on which the code wants to be executed.

**Typing

The first step was to implement the typing verification as the Parsing was already done with the class Parser. This is done by visiting recursively the syntax tree. We thought that it was easier to work with visiting methods that were returning Typed object instead of void methods that are only modifying global variables and shared fields. This is why we modified all the types of the visit methods of Typing class. 

This typer can type all the different structures and coding architectures available except the double assignement of fields such as:

s->a = s->b = e;

We are sure that we could solve this problem with more time but decided to move forward as it was more interesting for us to solve the next steps. 

**ToRTL

The second step and actually the first compilator step was to translate this typed code in RTL language. This is done throught the Translate method of ToRTL class. This method iterate over the declared typed functions and walks through the code of each one from the bottom in order to always know which label is coming next. This translator does not works perfectly well for the tests but translate well simple programms. 

**ToERTL

The following step is translation to ERTL, a slight improvement from RTL. The goal is to start working with registers that will concretely be use in x86-64 assembly language. This is done through the ToERTL class with the method Translate as before but in this case, instead of going through the code from the bottom, we decided to go through the tree via a depth-first search. The idea is to add the necessary instructions at the beginning and ending of functions and then to connected theses instructions to a graph that is the exact copy of the already existing one but translated in ERTL classes. 

**Limits and issues

During the whole project we encountered a lot of issues that were hard to surpass for us. 
The first one was a great mistake. We started to implement our compilator without testing each single step. For example we implemented all of the Typing class before even testing it on a minimal function. This caused two effects: it was very difficult to debug because of the complexity and the inevitable load of mistakes that came in this code and it pushed us to implementa exhaustive compilator for each step. 
The second one was the testing with the available tests. Understanding how to setup the Java project in VSCode, how to positionate the run file, etc... took us a lot of time because each step was messing with others and we did not know by which side take this problem.
In the RTL translation part the understanding of the implementation principles was the secret key that we missed for a while. The concept of working only with global variables and shared fields combined with the backward reading of the code was a painful source of bugs and problems. 
For the ERTL translation, it was quite straightforward and for the limited architectures that we decided to implement we did not encountered huge problems. 

**Conclusion

Finally, because of the deadline and the remaining time, we did not implement a complete compilator that can convert to assembly language any program in mini-c. This is mainly due to our ambitions of the beginning of the project that took us too much time and it feels very frustrating. However, this project really took its fully meaning after the written exam as it allowed us to anchor our knowledge and understanding of the concepts behind compilation.
