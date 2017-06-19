package de.charite.compbio.ontolib.ontology.similarity;

import de.charite.compbio.ontolib.ontology.data.Ontology;
import de.charite.compbio.ontolib.ontology.data.Term;
import de.charite.compbio.ontolib.ontology.data.TermId;
import de.charite.compbio.ontolib.ontology.data.TermRelation;
import java.util.Collection;


/**
 * Abstract base class for similarity measures computed based on information content of common
 * ancestors, such as Resnik, Lin, and Jiang similarities.
 *
 * @param <T> {@link Term} sub class to use in the contained classes
 * @param <R> {@link TermRelation} sub class to use in the contained classes
 *
 * @author <a href="mailto:manuel.holtgrewe@bihealth.de">Manuel Holtgrewe</a>
 * @author <a href="mailto:sebastian.koehler@charite.de">Sebastian Koehler</a>
 */
abstract class AbstractCommonAncestorSimilarity<T extends Term, R extends TermRelation>
    implements
      Similarity {

  // TODO: remove ontology?
  /**
   * {@link Ontology} underlying the computation.
   */
  private final Ontology<T, R> ontology;

  /** Whether or not to use symmetric score flavor (arithmetic mean of both directions). */
  private final boolean symmetric;

  /** Pairwise term similarity computation. */
  private final PairwiseSimilarity pairwiseSimilarity;

  /**
   * Constructor.
   * 
   * @param ontology {@link Ontology} to base computations on.
   * @param symmetric Whether or not to compute score in symmetric fashion.
   * @param pairwiseSimilarity {@link PairwiseSimilarity} to use for constructing set-to-set
   *        similarity
   */
  public AbstractCommonAncestorSimilarity(Ontology<T, R> ontology, boolean symmetric,
      PairwiseSimilarity pairwiseSimilarity) {
    this.ontology = ontology;
    this.symmetric = symmetric;
    this.pairwiseSimilarity = pairwiseSimilarity;
  }

  @Override
  public final double computeScore(Collection<TermId> query, Collection<TermId> target) {
    if (symmetric) {
      return 0.5 * (computeScoreImpl(query, target) + computeScoreImpl(target, query));
    } else {
      return computeScoreImpl(query, target);
    }
  }

  /**
   * Compute directed score between a query and a target set of {@link TermId}s.
   *
   * @param query Query set of {@link TermId}s.
   * @param target Target set of {@link TermId}s
   * @return Symmetric similarity score between <code>query</code> and <code>target</code>.
   */
  protected final double computeScoreImpl(Collection<TermId> query, Collection<TermId> target) {
    double sum = 0;

    for (TermId q : query) {
      double maxValue = 0.0;
      for (TermId t : target) {
        maxValue = Math.max(maxValue, pairwiseSimilarity.computeScore(q, t));
      }
      sum += maxValue;
    }

    return sum / query.size();
  }

  /**
   * @return Whether score computation is to be symmetric.
   */
  public final boolean isSymmetric() {
    return symmetric;
  }

  public String getParameters() {
    return "{symmetric: " + this.isSymmetric() + "}";
  }

}
