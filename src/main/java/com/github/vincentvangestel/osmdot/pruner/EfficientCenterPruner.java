package com.github.vincentvangestel.osmdot.pruner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.math3.random.MersenneTwister;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.PathNotFoundException;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.ImmutableList;

public class EfficientCenterPruner implements Pruner {

	/**
	 * Prunes all nodes, unreachable from the center most point from the graph. The supplied graph is modified (no copy is taken)
	 * @param g The graph to be pruned
	 * @return The modified graph
	 */
	@Override
	public Graph<MultiAttributeData> prune(Graph<MultiAttributeData> g) {
		Point center = getCenterMostPoint(g);
		
		Set<Point> nodesReachableFromCenter = new HashSet<Point>();
		Set<Point> edgeNodes = new HashSet<Point>();
		Set<Point> tovisitNodes = new HashSet<Point>();
		
		nodesReachableFromCenter.add(center);
		edgeNodes.add(center);
		
		int progress = 0;
		final int numberOfNodes = g.getNumberOfNodes();
		System.out.println("EfficientCenterPruning... ("+numberOfNodes+")");
		System.out.println("-getting reachable nodes from center");
		
		while(!edgeNodes.isEmpty()) {
			for (Point edgenode : edgeNodes) {
				progress ++;
				if (progress%1000 == 0)
					System.out.print("\r" + (progress*100/numberOfNodes) +"%");
				
				for (Point outGoingPoint : g.getOutgoingConnections(edgenode)) {
					if (!nodesReachableFromCenter.contains(outGoingPoint)) {
						nodesReachableFromCenter.add(outGoingPoint);
						tovisitNodes.add(outGoingPoint);
					}
				}				
			}
			edgeNodes.clear();
			edgeNodes.addAll(tovisitNodes);
			tovisitNodes.clear();
		}

		System.out.println("\n-removing nodes unreachable from center " + (numberOfNodes-nodesReachableFromCenter.size()));
		progress = 0;
		for(Point node : g.getNodes()) {
			progress ++;
			if (progress%1000 == 0)
				System.out.print("\r" + (progress*100/numberOfNodes) +"%");
			
			if (!nodesReachableFromCenter.contains(node))
				g.removeNode(node);
		}
		
		Set<Point> nodesCanReachCenter = new HashSet<Point>();
		nodesCanReachCenter.add(center);
		
		final int nbreachablefromcenter = nodesReachableFromCenter.size();
		System.out.println("\n-getting nodes that can reach the center " + nbreachablefromcenter);
		progress = 0;
		for (Point point : nodesReachableFromCenter) {
			progress ++;
			if (progress%1000 == 0)
				System.out.print("\r" + (progress*100/nbreachablefromcenter) +"%");
			
			if (canReachNodes(g, point, nodesCanReachCenter))
				nodesCanReachCenter.add(point);
		}
		
		System.out.println("\n-removing nodes that can not reach the center " + (nbreachablefromcenter-nodesCanReachCenter.size()));
		progress = 0;
		for(Point node : nodesReachableFromCenter) {
			progress ++;
			if (progress%1000 == 0)
				System.out.print("\r" + (progress*100/nbreachablefromcenter) +"%");
			
			if (!nodesCanReachCenter.contains(node))
				g.removeNode(node);
		}
		
		
		System.out.println("...DONE");
		
		Logger.getGlobal().info("EfficientCenterPruner pruned " + (numberOfNodes-nodesReachableFromCenter.size()) + " nodes from the graph");
		return g;
	}

	
	  private boolean canReachNodes(Graph<MultiAttributeData> g, Point point, Set<Point> nodesCanReachCenter) {
			
	  	Set<Point> nodesReachableFromPoint = new HashSet<Point>();
	  	Set<Point> edgeNodes = new HashSet<Point>();
	  	Set<Point> tovisitNodes = new HashSet<Point>();
	  	
	  	nodesReachableFromPoint.add(point);
	  	edgeNodes.add(point);
	  	
	  	while(!edgeNodes.isEmpty()) {
	  		for (Point edgenode : edgeNodes) {
	  			for (Point outGoingPoint : g.getOutgoingConnections(edgenode)) {
	  				if (nodesCanReachCenter.contains(outGoingPoint))
	  					return true;
					if (!nodesReachableFromPoint.contains(outGoingPoint)) {
						nodesReachableFromPoint.add(outGoingPoint);
						tovisitNodes.add(outGoingPoint);
					}
				}				
			}
			edgeNodes.clear();
			edgeNodes.addAll(tovisitNodes);
			tovisitNodes.clear();
		}
		return false;
	}


	/**
	   * Returns the point closest to the exact center of the area spanned by the
	   * graph.
	   * @param graph The graph.
	   * @return The point of the graph closest to the exact center of the area
	   *         spanned by the graph.
	   */
	  private Point getCenterMostPoint(Graph<?> graph) {
	    final ImmutableList<Point> extremes = Graphs.getExtremes(graph);
	    final Point exactCenter =
	      Point.divide(Point.add(extremes.get(0), extremes.get(1)), 2d);
	    Point center = graph.getRandomNode(new MersenneTwister());
	    double distance = Point.distance(center, exactCenter);

	    for (final Point p : graph.getNodes()) {
	      final double pDistance = Point.distance(p, exactCenter);
	      if (pDistance < distance) {
	        center = p;
	        distance = pDistance;
	      }

	      if (center.equals(exactCenter)) {
	        return center;
	      }
	    }

	    return center;
	  }

}
