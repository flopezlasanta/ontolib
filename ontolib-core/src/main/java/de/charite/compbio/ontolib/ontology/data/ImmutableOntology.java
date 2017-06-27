package de.charite.compbio.ontolib.ontology.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;
import de.charite.compbio.ontolib.graph.algo.BreadthFirstSearch;
import de.charite.compbio.ontolib.graph.algo.VertexVisitor;
import de.charite.compbio.ontolib.graph.data.DirectedGraph;
import de.charite.compbio.ontolib.graph.data.Edge;
import de.charite.compbio.ontolib.graph.data.ImmutableDirectedGraph;
import de.charite.compbio.ontolib.graph.data.ImmutableEdge;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of an immutable {@link Ontology}.
 *
 * @param <T> Type to use for terms.
 * @param <R> Type to use for term relations.
 *
 * @author <a href="mailto:manuel.holtgrewe@bihealth.de">Manuel Holtgrewe</a>
 */
public class ImmutableOntology<T extends Term, R extends TermRelation> implements Ontology<T, R> {

  /** Serial UId for serialization. */
  private static final long serialVersionUID = 1L;

  /** Meta information, as loaded from file. */
  private final ImmutableSortedMap<String, String> metaInfo;

  /** The graph storing the ontology's structure. */
  private final ImmutableDirectedGraph<TermId, ImmutableEdge<TermId>> graph;

  /** Id of the root term. */
  private final TermId rootTermId;

  /** The mapping from TermId to Term for non-obsolete terms. */
  private final ImmutableMap<TermId, T> termMap;

  /** The mapping from TermId to Term for defined by obsolete terms. */
  private final ImmutableMap<TermId, T> obsoleteTermMap;

  /** Set of all term IDs. */
  private final ImmutableSet<TermId> allTermIds;

  /** The mapping from edge Id to TermRelation. */
  private final ImmutableMap<Integer, R> relationMap;

  // TODO: should this be part of immutable ontology?
  /** Precomputed ancestors (including vertex itself). */
  private final ImmutableMap<TermId, ImmutableSet<TermId>> precomputedAncestors;

  /**
   * Constructor.
   *
   * @param metaInfo {@link ImmutableMap} with meta information.
   * @param graph Graph to use for underlying structure.
   * @param rootTermId Root node's {@link TermId}.
   * @param termMap Mapping from {@link TermId} to <code>T</code>, excluding obsolete ones.
   * @param obsoleteTermMap Mapping from {@link TermId} to <code>T</code>, only obsolete ones.
   * @param relationMap Mapping from numeric edge Id to <code>R</code>.
   */
  public ImmutableOntology(ImmutableSortedMap<String, String> metaInfo,
      ImmutableDirectedGraph<TermId, ImmutableEdge<TermId>> graph, TermId rootTermId,
      ImmutableMap<TermId, T> termMap, ImmutableMap<TermId, T> obsoleteTermMap,
      ImmutableMap<Integer, R> relationMap) {
    this.metaInfo = metaInfo;
    this.graph = graph;
    this.rootTermId = rootTermId;
    this.termMap = termMap;
    this.obsoleteTermMap = obsoleteTermMap;
    this.allTermIds = ImmutableSet.copyOf(Sets.union(termMap.keySet(), obsoleteTermMap.keySet()));
    this.relationMap = relationMap;
    this.precomputedAncestors = precomputeAncestors();
  }

  /**
   * @return Precomputed map from term id to list of ancestor term ids (a term is its own ancestor).
   */
  private ImmutableMap<TermId, ImmutableSet<TermId>> precomputeAncestors() {
    final ImmutableMap.Builder<TermId, ImmutableSet<TermId>> mapBuilder = ImmutableMap.builder();

    for (TermId termId : graph.getVertices()) {
      final ImmutableSet.Builder<TermId> setBuilder = ImmutableSet.builder();
      BreadthFirstSearch<TermId, ImmutableEdge<TermId>> bfs = new BreadthFirstSearch<>();
      bfs.startFromForward(graph, termId, new VertexVisitor<TermId, ImmutableEdge<TermId>>() {
        @Override
        public boolean visit(DirectedGraph<TermId, ImmutableEdge<TermId>> g, TermId v) {
          setBuilder.add(v);
          return true;
        }
      });

      mapBuilder.put(termId, setBuilder.build());
    }

    return mapBuilder.build();
  }

  @Override
  public Map<String, String> getMetaInfo() {
    return metaInfo;
  }

  @Override
  public DirectedGraph<TermId, ? extends Edge<TermId>> getGraph() {
    return graph;
  }

  @Override
  public Map<TermId, T> getTermMap() {
    return termMap;
  }

  @Override
  public Map<Integer, R> getRelationMap() {
    return relationMap;
  }

  @Override
  public boolean isRootTerm(TermId termId) {
    return termId.equals(rootTermId);
  }

  @Override
  public Set<TermId> getAncestors(TermId termId, boolean includeRoot) {
    if (includeRoot) {
      return precomputedAncestors.get(termId);
    } else {
      return ImmutableSet
          .copyOf(Sets.difference(precomputedAncestors.get(termId), ImmutableSet.of(rootTermId)));
    }
  }

  @Override
  public Set<TermId> getAllAncestorTermIds(Collection<TermId> termIds, boolean includeRoot) {
    final Set<TermId> result = new HashSet<>();
    for (TermId termId : termIds) {
      result.addAll(getAncestors(termId, true));
    }
    if (!includeRoot) {
      result.remove(rootTermId);
    }
    return result;
  }

  @Override
  public TermId getRootTermId() {
    return rootTermId;
  }

  @Override
  public Collection<TermId> getAllTermIds() {
    return allTermIds;
  }

  @Override
  public Collection<T> getTerms() {
    return termMap.values();
  }

  @Override
  public int countTerms() {
    return termMap.size();
  }

  @Override
  public Map<TermId, T> getObsoleteTermMap() {
    return obsoleteTermMap;
  }

  // TODO: Add "getAllTermMap()"?

  @Override
  public Collection<TermId> getNonObsoleteTermIds() {
    return termMap.keySet();
  }

  @Override
  public Collection<TermId> getObsoleteTermIds() {
    return obsoleteTermMap.keySet();
  }

}
