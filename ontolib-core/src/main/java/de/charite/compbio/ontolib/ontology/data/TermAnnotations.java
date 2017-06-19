package de.charite.compbio.ontolib.ontology.data;

import com.google.common.collect.Sets;
import de.charite.compbio.ontolib.ontology.algo.InformationContentComputation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class with static convenience functions.
 *
 * @author <a href="mailto:manuel.holtgrewe@bihealth.de">Manuel Holtgrewe</a>
 * @author <a href="mailto:sebastian.koehler@charite.de">Sebastian Koehler</a>
 */
public final class TermAnnotations {

  /**
   * Construct mapping from {@link TermId} to {@link Collection} of "world object" labels from
   * {@link Collection} of {@link TermAnnotation}s.
   *
   * <p>
   * Use this function for converting results from parsing term annotations to the appropriate
   * mapping for {@link InformationContentComputation}, for example.
   * </p>
   *
   * @param annotations {@link Collection} of {@link TermAnnotation}s to convert.
   * @return Constructed {@link Map} from {@link TermId} to {@link Collection} of "world object"
   *         labels.
   */
  public static Map<TermId, Collection<String>> constructTermAnnotationToLabelsMap(
      Collection<TermAnnotation> annotations) {
    final Map<TermId, Collection<String>> result = new HashMap<>();

    for (TermAnnotation anno : annotations) {
      if (!result.containsKey(anno.getTermId())) {
        result.put(anno.getTermId(), Sets.newHashSet(anno.getLabel()));
      } else {
        result.get(anno.getTermId()).add(anno.getLabel());
      }
    }

    return result;
  }

}