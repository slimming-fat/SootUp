package de.upb.swt.soot.core.jimple.common.stmt;

import de.upb.swt.soot.core.jimple.basic.*;
import de.upb.swt.soot.core.jimple.common.expr.AbstractInvokeExpr;
import de.upb.swt.soot.core.jimple.common.ref.JArrayRef;
import de.upb.swt.soot.core.jimple.common.ref.JFieldRef;
import de.upb.swt.soot.core.jimple.visitor.Acceptor;
import de.upb.swt.soot.core.jimple.visitor.Visitor;
import de.upb.swt.soot.core.util.Copyable;
import de.upb.swt.soot.core.util.printer.StmtPrinter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

public abstract class Stmt implements EquivTo, Acceptor, Copyable {

  /** List of Units pointing to this Stmt. */
  @Nonnull private List<Stmt> stmtsPointingToThis = new ArrayList<>();

  /**
   * Returns a list of Values used in this Unit. Note that they are returned in usual evaluation
   * order.
   */
  @Nonnull
  public List<Value> getUses() {
    return Collections.emptyList();
  }

  /** Returns a list of Values defined in this Unit. */
  @Nonnull
  public List<Value> getDefs() {
    return Collections.emptyList();
  }

  /** Returns a list of Units defined in this Unit; typically branch targets. */
  @Nonnull
  public List<Stmt> getStmts() {
    return Collections.emptyList();
  }

  /** Returns a list of units pointing to this Unit. */
  @Nonnull
  public List<Stmt> getStmtsPointingToThis() {
    return Collections.unmodifiableList(stmtsPointingToThis);
  }

  @Deprecated
  private void addStmtPointingToThis(Stmt stmt) {
    stmtsPointingToThis.add(stmt);
  }

  @Deprecated
  private void removeStmtPointingToThis(Stmt stmt) {
    stmtsPointingToThis.remove(stmt);
  }

  /** Returns a list of Values, either used or defined or both in this Unit. */
  @Nonnull
  public List<Value> getUsesAndDefs() {
    List<Value> uses = getUses();
    List<Value> defs = getDefs();
    if (uses.isEmpty()) {
      return defs;
    } else if (defs.isEmpty()) {
      return uses;
    } else {
      List<Value> values = new ArrayList<>();
      values.addAll(defs);
      values.addAll(uses);
      return values;
    }
  }

  /**
   * Returns true if execution after this statement may continue at the following statement. (e.g.
   * GotoStmt will return false and IfStmt will return true).
   */
  public abstract boolean fallsThrough();

  /**
   * Returns true if execution after this statement does not necessarily continue at the following
   * statement. GotoStmt and IfStmt will both return true.
   */
  public abstract boolean branches();

  public abstract void toString(StmtPrinter up);

  /** Used to implement the Switchable construct. */
  public void accept(Visitor sw) {}

  public boolean containsInvokeExpr() {
    return false;
  }

  public AbstractInvokeExpr getInvokeExpr() {
    throw new RuntimeException("getInvokeExpr() called with no invokeExpr present!");
  }

  public boolean containsArrayRef() {
    return false;
  }

  public JArrayRef getArrayRef() {
    throw new RuntimeException("getArrayRef() called with no ArrayRef present!");
  }

  public boolean containsFieldRef() {
    return false;
  }

  public JFieldRef getFieldRef() {
    throw new RuntimeException("getFieldRef() called with no JFieldRef present!");
  }

  public abstract StmtPositionInfo getPositionInfo();

  public boolean isBranchTarget() {
    return !stmtsPointingToThis.isEmpty();
  }

  /** This class is for internal use only. It will be removed in the future. */
  @Deprecated
  public static class $Accessor {
    // This class deliberately starts with a $-sign to discourage usage
    // of this Soot implementation detail.

    /** Violates immutability. Only use this for legacy code. */
    @Deprecated
    public static void addStmtPointingToThis(Stmt targetStmt, Stmt fromStmt) {
      targetStmt.addStmtPointingToThis(fromStmt);
    }

    /** Violates immutability. Only use this for legacy code. */
    @Deprecated
    public static void removeStmtPointingToThis(Stmt targetStmt, Stmt fromStmt) {
      targetStmt.removeStmtPointingToThis(fromStmt);
    }

    private $Accessor() {}
  }
}
