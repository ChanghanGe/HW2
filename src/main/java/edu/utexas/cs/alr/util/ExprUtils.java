package edu.utexas.cs.alr.util;

import edu.utexas.cs.alr.ast.*;
import edu.utexas.cs.alr.parser.ExprBaseListener;
import edu.utexas.cs.alr.parser.ExprLexer;
import edu.utexas.cs.alr.parser.ExprParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

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
        int varid = -1;
        Expr out = toCNF(mkEQUIV(mkVAR(varid), expr));
        varid = varid - 1;
        switch (expr.getKind())
        {
            case AND:
                AndExpr andExpr = (AndExpr) expr;
                Expr andleft = andExpr.getLeft();
                Expr andright = andExpr.getRight();
                out = mkAND(out, toCNF(mkEQUIV(mkVAR(varid), andleft)));
                varid = varid - 1;
                out = mkAND(out, toCNF(mkEQUIV(mkVAR(varid), andright)));
                varid = varid - 1;
                break;
            case NEG:   
                break;
            case VAR:
                break;
            case OR:
                break;
            default:
                assert false;
        }
        return out;
    }

    public static boolean checkSAT(Expr expr)
    {
        
        throw new UnsupportedOperationException("implement this");
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
        System.out.println(expr);
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
