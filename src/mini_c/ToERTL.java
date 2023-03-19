package mini_c;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

public class ToERTL extends EmptyRTLVisitor {

    
    ERTLfile ERfile;
    LinkedList<ERTLfun> ERfuns = new LinkedList<ERTLfun>();
    ERTLfun ERfun;
    int formals;
    Set<Register> locals;
    ERTLgraph graph;

    Label currentLabel;
    Label nextLabel;
    
    public ERTLfile translate(RTLfile file) {
        file.accept(this);
        return ERfile;
    }

    public void visit(RTLfile f) {
        ERfile = new ERTLfile();
        for(RTLfun fun: f.funs) {
            visit(fun);
        }
        ERfile.funs = ERfuns;
    }

    public void visit(RTLfun fun) {
        int nbArg = fun.formals.size();
        formals =  nbArg <= 6 ? nbArg : 6; 
        ERfun = new ERTLfun(fun.name, formals);

        graph.graph.put(fun.entry,new ERreturn());
        visit(fun.body);

        ERfun.body = graph;
        ERfuns.add(ERfun);
    }

    public void visit(Rconst c) {
        ERconst instr = new ERconst(c.i, c.r, c.l);
        graph.graph.put(nextLabel,instr);
    }

    public void visit(Rmbinop b) {
        ERmbinop instr = new ERmbinop(b.m, b.r1, b.r2, b.l);
        graph.graph.put(nextLabel,instr);
    }

    public void visit(RTLgraph g) {
        graph = new ERTLgraph();
        g.graph.forEach((label,instr) -> {
            nextLabel = label;
            instr.accept(this);
        });
    }
}
