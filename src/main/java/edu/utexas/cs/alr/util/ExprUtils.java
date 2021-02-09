package edu.utexas.cs.alr.util;

import edu.utexas.cs.alr.ast.*;
import edu.utexas.cs.alr.parser.ExprBaseListener;
import edu.utexas.cs.alr.parser.ExprLexer;
import edu.utexas.cs.alr.parser.ExprParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.graalvm.compiler.nodes.extended.GetClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

import javax.lang.model.util.ElementScanner6;

import static edu.utexas.cs.alr.ast.ExprFactory.*;

public class ExprUtils
{
    public static Expr toCNF(Expr expr)
    {
        Expr out = expr;
        switch (expr.getKind())
        {
            case AND:
                AndExpr andExpr = (AndExpr) expr;
                Expr andleft = toCNF(andExpr.getLeft());
                Expr andright = toCNF(andExpr.getRight());

                Expr.ExprKind andleftKind = andleft.getKind();
                Expr.ExprKind andrightKind = andright.getKind();

                if (andleftKind == Expr.ExprKind.OR && andrightKind == Expr.ExprKind.AND){
                    Expr andrightandleft = toCNF(((AndExpr) andright).getLeft());
                    Expr andrightandright = toCNF(((AndExpr) andright).getRight());
                    out = toCNF(mkAND(mkAND(andleft, andrightandleft),mkAND(andleft, andrightandright)));
                }
                else if (andleftKind == Expr.ExprKind.AND && andrightKind == Expr.ExprKind.OR){
                    Expr andleftandleft = toCNF(((AndExpr) andleft).getLeft());
                    Expr andleftandright = toCNF(((AndExpr) andleft).getRight());
                    out = toCNF(mkAND(mkAND(andleftandleft, andright),mkAND(andleftandright,andright)));
                }
                else{
                    out = mkAND(andleft, andright);
                    break;
                }
                break;

            case NEG:
                Expr negExpr = ((NegExpr) expr).getExpr();
                switch (negExpr.getKind())
                {
                    case AND:
                        Expr negexprandleft = mkNEG(((AndExpr) negExpr).getLeft());
                        Expr negexprandright = mkNEG(((AndExpr) negExpr).getRight());
                        out = toCNF((Expr) mkOR(negexprandleft, negexprandright));
                        break;
                    case OR:
                        Expr negexprorleft = mkNEG(((OrExpr) negExpr).getLeft());
                        Expr negexprorright = mkNEG(((OrExpr) negExpr).getRight());
                        out =  toCNF((Expr) mkAND(negexprorleft , negexprorright));
                        break;
                    case NEG:
                        out = toCNF(((NegExpr) negExpr).getExpr());
                        break;
                    case EQUIV:
                        Expr negexprequivleft = mkNEG(((EquivExpr) negExpr).getLeft());
                        Expr negexprequivright = mkNEG(((EquivExpr) negExpr).getRight());
                        Expr leftimplright = mkIMPL(negexprequivleft, negexprequivright);
                        Expr rightimplleft = mkIMPL(negexprequivright, negexprequivleft);
                        out =  toCNF((Expr) mkOR(mkNEG(leftimplright), mkNEG(rightimplleft)));
                        break;
                    case IMPL:
                        Expr negexprimplleft = ((ImplExpr) negExpr).getAntecedent();
                        Expr negexprimplright = mkNEG(((ImplExpr) negExpr).getConsequent());
                        out = toCNF((Expr) mkAND(negexprimplleft , negexprimplright));
                        break;
                    default:
                        assert false;
                }
                break;

            case VAR:
                break;

            case OR:
                OrExpr orexpr = (OrExpr) expr;
                Expr orleft = toCNF(orexpr.getLeft());
                Expr orright = toCNF(orexpr.getRight());
                // System.out.println('\n');
                // System.out.println(orleft);
                // System.out.println(orright);

                if (isLiteral(orleft) && isLiteral(orright)){
                    out = mkOR(orleft, orright);
                    break;
                }
                else if (isLiteral(orleft) && !isLiteral(orright)){
                    switch (orright.getKind())
                    {
                        case AND:
                            Expr orrightandleft = toCNF(((AndExpr) orright).getLeft());
                            Expr orrightandright = toCNF(((AndExpr) orright).getRight());
                            out = toCNF(mkAND(mkOR(orleft, orrightandleft), mkOR(orleft, orrightandright)));
                            break;
                        case OR:
                            out = mkOR(orleft, orright);
                            break;
                        default:
                            assert false;
                    }
                }
                else if (!isLiteral(orleft) && isLiteral(orright)){
                    switch (orleft.getKind())
                    {
                        case AND:
                            Expr orleftandleft = toCNF(((AndExpr) orleft).getLeft());
                            Expr orleftandright = toCNF(((AndExpr) orleft).getRight());
                            out = toCNF(mkAND(mkOR(orleftandleft, orright), mkOR(orleftandright, orright)));
                            break;
                        case OR:
                            out = mkOR(orleft, orright);
                            break;
                        default:
                            assert false;
                    }
                }
                else{
                    Expr.ExprKind leftKind = orleft.getKind();
                    Expr.ExprKind rightKind = orright.getKind();

                    if (leftKind == Expr.ExprKind.AND && rightKind == Expr.ExprKind.AND){
                        Expr orleftandleft = toCNF(((AndExpr) orleft).getLeft());
                        Expr orleftandright = toCNF(((AndExpr) orleft).getRight());
                        Expr orrightandleft = toCNF(((AndExpr) orright).getLeft());
                        Expr orrightandright = toCNF(((AndExpr) orright).getRight());
                        Expr out1 = mkAND(mkOR(orleftandleft, orrightandleft),mkOR(orleftandleft, orrightandright));
                        Expr out2 = mkAND(mkOR(orleftandright, orrightandleft),mkOR(orleftandright, orrightandright));
                        out = toCNF(mkAND(out1, out2));
                    }
                    else if (leftKind == Expr.ExprKind.OR && rightKind == Expr.ExprKind.AND){
                        Expr orrightandleft = toCNF(((AndExpr) orright).getLeft());
                        Expr orrightandright = toCNF(((AndExpr) orright).getRight());
                        out = toCNF(mkAND(mkOR(orleft, orrightandleft),mkOR(orleft, orrightandright)));
                    }
                    else if (leftKind == Expr.ExprKind.AND && rightKind == Expr.ExprKind.OR){
                        Expr orleftandleft = toCNF(((AndExpr) orleft).getLeft());
                        Expr orleftandright = toCNF(((AndExpr) orleft).getRight());
                        out = toCNF(mkAND(mkOR(orleftandleft, orright),mkOR(orleftandright, orright)));
                    }
                    else{
                        out = mkOR(orleft, orright);
                        break;
                    }
                }
                break;

            case EQUIV:
                EquivExpr equivExpr = (EquivExpr) expr;
                Expr equivExprL = mkIMPL(equivExpr.getLeft(), equivExpr.getRight());
                Expr equivExprR = mkIMPL(equivExpr.getRight(), equivExpr.getLeft());
                out = toCNF((Expr) mkAND(equivExprL, equivExprR));
                break;

            case IMPL:
                ImplExpr implExpr = (ImplExpr) expr;
                out =  toCNF((Expr) mkOR(mkNEG(implExpr.getAntecedent()), implExpr.getConsequent()));
                break;

            default:
                assert false;
        }
        return out;
    }

    public static Expr toTseitin(Expr expr)
    {
        long newvarid = getLagestVarId(expr);
        if (newvarid==1)
            return expr;

        Stack<Expr> s = new Stack<>();
        Stack<Long> v = new Stack<>();


        Expr out = mkVAR(newvarid+1);
        s.push(expr);
        v.push(++newvarid);

        while (!s.isEmpty())
        {
            Expr e = s.pop();
            Long id = v.pop();

            switch (e.getKind())
            {
                case AND:
                    AndExpr andExpr = (AndExpr) e;
                    Expr andleft = andExpr.getLeft();
                    Expr andright = andExpr.getRight();
                    
                    if (andleft.getKind()!=Expr.ExprKind.VAR && andright.getKind()!=Expr.ExprKind.VAR)
                    {
                        s.push(andleft);
                        Long newandleftid = ++newvarid;
                        v.push(newandleftid);

                        s.push(andright);
                        Long newandrightid = ++newvarid;
                        v.push(newandrightid);

                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),mkAND(mkVAR(newandleftid), mkVAR(newandrightid)))));
                    }
                    else if (andleft.getKind()==Expr.ExprKind.VAR && andright.getKind()!=Expr.ExprKind.VAR)
                    {
                        s.push(andright);
                        Long newandrightid = ++newvarid;
                        v.push(newandrightid);

                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),mkAND(andleft, mkVAR(newandrightid)))));
                    }
                    else if (andleft.getKind()!=Expr.ExprKind.VAR && andright.getKind()==Expr.ExprKind.VAR)
                    {
                        s.push(andleft);
                        Long newandleftid = ++newvarid;
                        v.push(newandleftid);

                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),mkAND(mkVAR(newandleftid),andright))));
                    }
                    else
                    {
                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),e)));
                    }
                    break;
                case NEG:
                    Expr negExpr = ((NegExpr) e).getExpr();
                    if (negExpr.getKind()!=Expr.ExprKind.VAR){
                        s.push(negExpr);
                        Long newnegid = ++newvarid;
                        v.push(newnegid);
                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),mkNEG(mkVAR(newnegid)))));
                    }
                    else
                    {
                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),e)));
                    }
                        
                    break;
                case VAR:
                    out = mkAND(out, e);
                    break;
                case OR:
                    OrExpr orExpr = (OrExpr) e;
                    Expr orleft = orExpr.getLeft();
                    Expr orright = orExpr.getRight();
                    
                    if (orleft.getKind()!=Expr.ExprKind.VAR && orright.getKind()!=Expr.ExprKind.VAR)
                    {
                        s.push(orleft);
                        Long neworleftid = ++newvarid;
                        v.push(neworleftid);

                        s.push(orright);
                        Long neworrightid = ++newvarid;
                        v.push(neworrightid);

                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),mkOR(mkVAR(neworleftid), mkVAR(neworrightid)))));
                    }
                    else if (orleft.getKind()==Expr.ExprKind.VAR && orright.getKind()!=Expr.ExprKind.VAR)
                    {
                        s.push(orright);
                        Long neworrightid = ++newvarid;
                        v.push(neworrightid);

                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),mkOR(orleft, mkVAR(neworrightid)))));
                    }
                    else if (orleft.getKind()!=Expr.ExprKind.VAR && orright.getKind()==Expr.ExprKind.VAR)
                    {
                        s.push(orleft);
                        Long neworleftid = ++newvarid;
                        v.push(neworleftid);

                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),mkOR(mkVAR(neworleftid),orright))));
                    }
                    else
                    {
                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),e)));
                    }
                    break;
                case IMPL:
                    ImplExpr implExpr = (ImplExpr) e;
                    Expr implleft = implExpr.getAntecedent();
                    Expr implright = implExpr.getConsequent();

                    if (implleft.getKind()!=Expr.ExprKind.VAR && implright.getKind()!=Expr.ExprKind.VAR)
                    {
                        s.push(implleft);
                        Long newimplleftid = ++newvarid;
                        v.push(newimplleftid);

                        s.push(implright);
                        Long newimplrightid = ++newvarid;
                        v.push(newimplrightid);

                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),mkIMPL(mkVAR(newimplleftid), mkVAR(newimplrightid)))));
                    }
                    else if (implleft.getKind()==Expr.ExprKind.VAR && implright.getKind()!=Expr.ExprKind.VAR)
                    {
                        s.push(implright);
                        Long newimplrightid = ++newvarid;
                        v.push(newimplrightid);

                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),mkIMPL(implleft, mkVAR(newimplrightid)))));
                    }
                    else if (implleft.getKind()!=Expr.ExprKind.VAR && implright.getKind()==Expr.ExprKind.VAR)
                    {
                        s.push(implleft);
                        Long newimplleftid = ++newvarid;
                        v.push(newimplleftid);

                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),mkIMPL(mkVAR(newimplleftid),implright))));
                    }
                    else
                    {
                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),e)));
                    }
                    break;
                case EQUIV:
                    EquivExpr equivExpr = (EquivExpr) e;
                    Expr equivleft = equivExpr.getLeft();
                    Expr equivright = equivExpr.getRight();

                    if (equivleft.getKind()!=Expr.ExprKind.VAR && equivright.getKind()!=Expr.ExprKind.VAR)
                    {
                        s.push(equivleft);
                        Long newequivleftid = ++newvarid;
                        v.push(newequivleftid);

                        s.push(equivright);
                        Long newequivrightid = ++newvarid;
                        v.push(newequivrightid);

                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),mkEQUIV(mkVAR(newequivleftid), mkVAR(newequivrightid)))));
                    }
                    else if (equivleft.getKind()==Expr.ExprKind.VAR && equivright.getKind()!=Expr.ExprKind.VAR)
                    {
                        s.push(equivright);
                        Long newequivrightid = ++newvarid;
                        v.push(newequivrightid);

                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),mkEQUIV(equivleft, mkVAR(newequivrightid)))));
                    }
                    else if (equivleft.getKind()!=Expr.ExprKind.VAR && equivright.getKind()==Expr.ExprKind.VAR)
                    {
                        s.push(equivleft);
                        Long newequivleftid = ++newvarid;
                        v.push(newequivleftid);

                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),mkEQUIV(mkVAR(newequivleftid),equivright))));
                    }
                    else
                    {
                        out = mkAND(out, toCNF(mkEQUIV(mkVAR(id),e)));
                    }
                    break;
                default:
                    assert false;
            }
        }
        return out;
    }
    
    public static boolean checkSAT(Expr expr)
    {
        Set<Set<Long>> clauses = getClauses(expr);

        
        return true;
    }

    public static Set<Set<Long>> getClauses(Expr expr)
    {
        Expr eqsatExpr = toTseitin(expr);

        Set<Set<Long>> clauses = new HashSet<>();
        Set<Long> vars = new HashSet<>();

        Stack<Expr> s = new Stack<>();
        s.push(eqsatExpr);

        while (!s.isEmpty())
        {
            Expr e = s.pop();

            switch (e.getKind())
            {
                case AND:
                    AndExpr andExpr = (AndExpr) e;
                    s.push(andExpr.getLeft());
                    s.push(andExpr.getRight());
                    break;
                case NEG:
                    if (!isLiteral(e))
                        throw new RuntimeException("Expr is not in CNF.");

                    VarExpr childVarExpr = (VarExpr) ((NegExpr) e).getExpr();

                    clauses.add(Collections.singleton(-childVarExpr.getId()));
                    vars.add(childVarExpr.getId());
                    break;
                case VAR:
                    VarExpr varExpr = (VarExpr) e;
                    clauses.add(Collections.singleton(varExpr.getId()));
                    vars.add(varExpr.getId());
                    break;
                case OR:
                    clauses.add(getLiteralsForClause((OrExpr) e, vars));
                    break;
                default:
                    assert false;
            }
        }
        
        return clauses;
    }

    public static long getLagestVarId(Expr expr)
    {
        long maxid = 0;

        Stack<Expr> s = new Stack<>();

        Expr cnfExpr = toCNF(expr);

        s.push(cnfExpr);

        while (!s.isEmpty())
        {
            Expr e = s.pop();

            switch (e.getKind())
            {
                case AND:
                    AndExpr andExpr = (AndExpr) e;
                    s.push(andExpr.getLeft());
                    s.push(andExpr.getRight());
                    break;
                case NEG:
                    VarExpr childVarExpr = (VarExpr) ((NegExpr) e).getExpr();
                    if (childVarExpr.getId() > maxid)
                        maxid = childVarExpr.getId();
                    break;
                case VAR:
                    VarExpr varExpr = (VarExpr) e;
                    if (varExpr.getId() > maxid)
                        maxid = varExpr.getId();
                    break;
                case OR:
                    OrExpr orExpr = (OrExpr) e;
                    s.push(orExpr.getLeft());
                    s.push(orExpr.getRight());;
                    break;
                default:
                    assert false;
            }
        }
        return maxid;
    }

    public static Expr parseFrom(InputStream inStream) throws IOException
    {
        ExprLexer lexer = new ExprLexer(CharStreams.fromStream(inStream));
        BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
        ExprParser parser = new ExprParser(tokenStream);

        parser.addErrorListener(ThrowingErrorListener.INSTANCE);
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);

        ExprParser.ExprContext parseTree = parser.expr();
        ASTListener astListener = new ASTListener();
        ParseTreeWalker.DEFAULT.walk(astListener, parseTree);

        return astListener.pendingExpr.pop();
    }

    public static void printDimcas(Expr expr, PrintStream out)
    {
        //System.out.println(expr);
        Set<Set<Long>> clauses = new HashSet<>();
        Set<Long> vars = new HashSet<>();

        Stack<Expr> s = new Stack<>();
        s.push(expr);

        while (!s.isEmpty())
        {
            Expr e = s.pop();

            if (!canBeCNF(e))
                throw new RuntimeException("Expr is not in CNF.");

            switch (e.getKind())
            {
                case AND:
                    AndExpr andExpr = (AndExpr) e;
                    s.push(andExpr.getLeft());
                    s.push(andExpr.getRight());
                    break;
                case NEG:
                    if (!isLiteral(e))
                        throw new RuntimeException("Expr is not in CNF.");

                    VarExpr childVarExpr = (VarExpr) ((NegExpr) e).getExpr();

                    clauses.add(Collections.singleton(-childVarExpr.getId()));
                    vars.add(childVarExpr.getId());
                    break;
                case VAR:
                    VarExpr varExpr = (VarExpr) e;
                    clauses.add(Collections.singleton(varExpr.getId()));
                    vars.add(varExpr.getId());
                    break;
                case OR:
                    clauses.add(getLiteralsForClause((OrExpr) e, vars));
                    break;
                default:
                    assert false;
            }
        }

        out.println("p cnf " + vars.size() + " " + clauses.size());

        clauses.forEach(c -> {
            c.forEach(l -> out.print(l + " "));
            out.println(0);
        });
    }

    public static boolean canBeCNF(Expr e)
    {
        Expr.ExprKind eKind = e.getKind();
        return eKind != Expr.ExprKind.EQUIV &&
               eKind != Expr.ExprKind.IMPL;
    }

    public static boolean isLiteral(Expr e)
    {
        Expr.ExprKind eKind = e.getKind();
        if (eKind == Expr.ExprKind.VAR)
            return true;

        if (eKind == Expr.ExprKind.NEG)
        {
            return ((NegExpr) e).getExpr().getKind() == Expr.ExprKind.VAR;
        }

        return false;
    }

    private static Set<Long> getLiteralsForClause(OrExpr orExpr, Set<Long> vars)
    {
        Set<Long> literals = new HashSet<>();
        Stack<Expr> s = new Stack<>();
        s.add(orExpr.getLeft());
        s.add(orExpr.getRight());

        while (!s.isEmpty())
        {
            Expr e = s.pop();

            if (e.getKind() != Expr.ExprKind.OR && !isLiteral(e))
                throw new RuntimeException("Expr is not in CNF");

            switch (e.getKind())
            {
                case OR:
                    OrExpr or = (OrExpr) e;
                    s.push(or.getLeft());
                    s.push(or.getRight());
                    break;
                case VAR:
                    long varId = ((VarExpr) e).getId();
                    literals.add(varId);
                    vars.add(varId);
                    break;
                case NEG:
                    NegExpr neg = (NegExpr) e;
                    long litId = -((VarExpr)neg.getExpr()).getId();
                    literals.add(litId);
                    vars.add(-litId);
                    break;
                default:
                    assert false;
            }
        }
        return literals;
    }
}

class ASTListener extends ExprBaseListener
{
    Stack<Expr> pendingExpr = new Stack<>();

    @Override
    public void exitAtom(ExprParser.AtomContext ctx)
    {
        long id = Long.parseLong(ctx.VAR().toString().substring(1));
        VarExpr var = mkVAR(id);

        pendingExpr.push(var);
    }

    @Override
    public void exitLneg(ExprParser.LnegContext ctx)
    {
        Expr expr = pendingExpr.pop();
        NegExpr negExpr = mkNEG(expr);

        pendingExpr.push(negExpr);
    }

    @Override
    public void exitLand(ExprParser.LandContext ctx)
    {
        Expr right = pendingExpr.pop(), left = pendingExpr.pop();
        AndExpr andExpr = mkAND(left, right);

        pendingExpr.push(andExpr);
    }

    @Override
    public void exitLor(ExprParser.LorContext ctx)
    {
        Expr right = pendingExpr.pop(), left = pendingExpr.pop();
        OrExpr orExpr = mkOR(left, right);

        pendingExpr.push(orExpr);
    }

    @Override
    public void exitLimpl(ExprParser.LimplContext ctx)
    {
        Expr consequent = pendingExpr.pop(), antecedent = pendingExpr.pop();
        ImplExpr implExpr = mkIMPL(antecedent, consequent);

        pendingExpr.push(implExpr);
    }

    @Override
    public void exitLequiv(ExprParser.LequivContext ctx)
    {
        Expr right = pendingExpr.pop(), left = pendingExpr.pop();
        EquivExpr equivExpr = mkEQUIV(left, right);

        pendingExpr.push(equivExpr);
    }
}

class ThrowingErrorListener extends BaseErrorListener
{

    public static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
            throws ParseCancellationException
    {
        throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
    }
}
