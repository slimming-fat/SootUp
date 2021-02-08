package de.upb.swt.soot.callgraph.spark.pag;

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

import de.upb.swt.soot.callgraph.spark.builder.MethodNodeFactory;
import de.upb.swt.soot.callgraph.spark.pag.nodes.Node;
import de.upb.swt.soot.core.jimple.common.stmt.Stmt;
import de.upb.swt.soot.core.model.Body;
import de.upb.swt.soot.core.model.SootMethod;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class IntraproceduralPointerAssignmentGraph {

  private static final Logger log = LoggerFactory.getLogger(IntraproceduralPointerAssignmentGraph.class);

  private PointerAssignmentGraph pag;
  private DefaultDirectedGraph<SparkVertex, SparkEdge> graph;
  private SootMethod method;
  private MethodNodeFactory nodeFactory;
  private boolean isBuilt = false;
  private boolean isAdded =  false;



  public IntraproceduralPointerAssignmentGraph(PointerAssignmentGraph pag, SootMethod method) {
    this.pag = pag;
    this.method = method;
    this.nodeFactory = new MethodNodeFactory(this);
    this.graph = new DefaultDirectedGraph<>(SparkEdge.class);
    build();
  }

  public void build() {
    if (method.isConcrete()) {
      Body body = method.getBody();
      for (Stmt stmt : body.getStmts()) {
        nodeFactory.processStmt(stmt);
      }
    } else {
      // TODO: build for native
    }
    //TODO: addMiscEdges
  }

  public DefaultDirectedGraph<SparkVertex, SparkEdge> getGraph() {
    return graph;
  }

  public SootMethod getMethod() {
    return method;
  }

  public PointerAssignmentGraph getPointerAssignmentGraph() {
    return pag;
  }

  public void addEdge(Node source, Node target){
    if(source == null){
      return;
    }
    SparkVertex src = new SparkVertex(source);
    SparkVertex trg = new SparkVertex(target);
    graph.addVertex(src);
    graph.addVertex(trg);
    SparkEdgeFactory edgeFactory = new SparkEdgeFactory();
    SparkEdge edge = edgeFactory.getEdge(source, target);
    graph.addEdge(src, trg, edge);
    log.info("Added {} edge from:{} to:{}", edge.getEdgeType(), source, target);
  }

}