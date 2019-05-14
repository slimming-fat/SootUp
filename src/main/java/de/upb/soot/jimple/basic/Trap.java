package de.upb.soot.jimple.basic;

import de.upb.soot.jimple.common.stmt.IStmt;
import de.upb.soot.types.JavaClassType;
import de.upb.soot.util.Copyable;

/**
 * A trap is an exception catcher.
 *
 * @author Linghui Luo
 */
public interface Trap extends StmtBoxOwner, Copyable {

  JavaClassType getException();

  IStmt getBeginStmt();

  IStmt getEndStmt();

  IStmt getHandlerStmt();
}
