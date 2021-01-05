package de.upb.swt.soot.callgraph.spark.sets;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 2002-2021 Ondrej Lhotak, Kadiray Karakaya and others
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import de.upb.swt.soot.callgraph.spark.pag.Node;
import de.upb.swt.soot.callgraph.spark.pag.PAG;
import de.upb.swt.soot.callgraph.spark.pointsto.PointsToSet;
import de.upb.swt.soot.core.jimple.common.constant.ClassConstant;
import de.upb.swt.soot.core.types.Type;
import jdk.nashorn.internal.runtime.BitVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.jvm.hotspot.debugger.cdbg.RefType;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class for implementations of points-to sets.
 *
 * @author Kadiray Karakaya
 */
public abstract class PointsToSetInternal implements EqualsSupportingPointsToSet {
    private static final Logger logger = LoggerFactory.getLogger(PointsToSetInternal.class);

    /** Calls v's visit method on all nodes in this set. */
    public abstract boolean forall(P2SetVisitor v);

    /** Adds n to this set, returns true if n was not already in this set. */
    public abstract boolean add(Node n);

    /** Returns set of newly-added nodes since last call to flushNew. */
    public PointsToSetInternal getNewSet() {
        return this;
    }

    /** Returns set of nodes already present before last call to flushNew. */
    public PointsToSetInternal getOldSet() {
        return EmptyPointsToSet.v();
    }

    /** Sets all newly-added nodes to old nodes. */
    public void flushNew() {
    }

    /** Sets all nodes to newly-added nodes. */
    public void unFlushNew() {
    }

    /** Merges other into this set. */
    public void mergeWith(PointsToSetInternal other) {
        addAll(other, null);
    }

    /** Returns true iff the set contains n. */
    public abstract boolean contains(Node n);

    public PointsToSetInternal(Type type) {
        this.type = type;
    }

    public boolean hasNonEmptyIntersection(PointsToSet other) {
        if (other instanceof PointsToSetInternal) {
            final PointsToSetInternal o = (PointsToSetInternal) other;
            return forall(new P2SetVisitor() {
                public void visit(Node n) {
                    if (o.contains(n)) {
                        returnValue = true;
                    }
                }
            });
        } else if (other instanceof FullObjectSet) {
            final FullObjectSet fos = (FullObjectSet) other;
            return fos.possibleTypes().contains(type);
        } else {
            // We don't really know
            return false;
        }
    }

    public Set<Type> possibleTypes() {
        final HashSet<Type> ret = new HashSet<>();
        forall(new P2SetVisitor() {
            public void visit(Node n) {
                Type t = n.getType();
                if (t instanceof RefType) {
                    RefType rt = (RefType) t;
                    if (rt.getSootClass().isAbstract()) {
                        return;
                    }
                }
                ret.add(t);
            }
        });
        return ret;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int size() {
        final int[] ret = new int[1];
        forall(new P2SetVisitor() {
            public void visit(Node n) {
                ret[0]++;
            }
        });
        return ret[0];
    }

    public String toString() {
        final StringBuffer ret = new StringBuffer();
        this.forall(new P2SetVisitor() {
            public final void visit(Node n) {
                ret.append("" + n + ",");
            }
        });
        return ret.toString();
    }

    public Set<String> possibleStringConstants() {
        final HashSet<String> ret = new HashSet<String>();
        return this.forall(new P2SetVisitor() {
            public final void visit(Node n) {
                if (n instanceof StringConstantNode) {
                    ret.add(((StringConstantNode) n).getString());
                } else {
                    returnValue = true;
                }
            }
        }) ? null : ret;
    }

    public Set<ClassConstant> possibleClassConstants() {
        final HashSet<ClassConstant> ret = new HashSet<ClassConstant>();
        return this.forall(new P2SetVisitor() {
            public final void visit(Node n) {
                if (n instanceof ClassConstantNode) {
                    ret.add(((ClassConstantNode) n).getClassConstant());
                } else {
                    returnValue = true;
                }
            }
        }) ? null : ret;
    }

    /* End of public methods. */
    /* End of package methods. */

    protected Type type;

    // Added by Adam Richard
    protected BitVector getBitMask(PointsToSetInternal other, PAG pag) {
        /*
         * Prevents propogating points-to sets of inappropriate type. E.g. if you have in the code being analyzed: Shape s =
         * (Circle)c; then the points-to set of s is only the elements in the points-to set of c that have type Circle.
         */
        // Code ripped from BitPointsToSet

        BitVector mask = null;
        TypeManager typeManager = pag.getTypeManager();
        if (!typeManager.castNeverFails(other.getType(), this.getType())) {
            mask = typeManager.get(this.getType());
        }
        return mask;
    }

    /**
     * {@inheritDoc}
     */
    public int pointsToSetHashCode() {
        P2SetVisitorInt visitor = new P2SetVisitorInt(1) {

            final int PRIME = 31;

            public void visit(Node n) {
                intValue = PRIME * intValue + n.hashCode();
            }

        };
        this.forall(visitor);
        return visitor.intValue;
    }

    /**
     * {@inheritDoc}
     */
    public boolean pointsToSetEquals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PointsToSetInternal)) {
            return false;
        }
        PointsToSetInternal otherPts = (PointsToSetInternal) other;

        // both sets are equal if they are supersets of each other
        return superSetOf(otherPts, this) && superSetOf(this, otherPts);
    }

    /**
     * Returns <code>true</code> if <code>onePts</code> is a (non-strict) superset of <code>otherPts</code>.
     */
    private boolean superSetOf(PointsToSetInternal onePts, final PointsToSetInternal otherPts) {
        return onePts.forall(new P2SetVisitorDefaultTrue() {

            public final void visit(Node n) {
                returnValue = returnValue && otherPts.contains(n);
            }

        });
    }

    /**
     * A P2SetVisitor with a default return value of <code>true</code>.
     *
     * @author Eric Bodden
     */
    public static abstract class P2SetVisitorDefaultTrue extends P2SetVisitor {

        public P2SetVisitorDefaultTrue() {
            returnValue = true;
        }

    }

    /**
     * A P2SetVisitor with an int value.
     *
     * @author Eric Bodden
     */
    public static abstract class P2SetVisitorInt extends P2SetVisitor {

        protected int intValue;

        public P2SetVisitorInt(int i) {
            intValue = 1;
        }

    }

}