package com.github.phenomics.ontolib.cli;

import com.github.phenomics.ontolib.base.OntoLibException;
import com.github.phenomics.ontolib.formats.hpo.HpoGeneAnnotation;
import com.github.phenomics.ontolib.formats.hpo.HpoOntology;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.io.base.TermAnnotationParserException;
import com.github.phenomics.ontolib.io.obo.hpo.HpoGeneAnnotationParser;
import com.github.phenomics.ontolib.io.obo.hpo.HpoOboParser;
import com.github.phenomics.ontolib.io.scoredist.ScoreDistributionWriter;
import com.github.phenomics.ontolib.io.scoredist.TextFileScoreDistributionWriter;
import com.github.phenomics.ontolib.ontology.algo.InformationContentComputation;
import com.github.phenomics.ontolib.ontology.data.ImmutableOntology;
import com.github.phenomics.ontolib.ontology.data.TermAnnotations;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.github.phenomics.ontolib.ontology.scoredist.ScoreDistribution;
import com.github.phenomics.ontolib.ontology.scoredist.ScoreSamplingOptions;
import com.github.phenomics.ontolib.ontology.scoredist.SimilarityScoreSampling;
import com.github.phenomics.ontolib.ontology.similarity.ResnikSimilarity;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@code precompute-scores} command.
 *
 * @author <a href="mailto:manuel.holtgrewe@bihealth.de">Manuel Holtgrewe</a>
 */
public class PrecomputeScoresCommand {

  /**
   * {@link Logger} object to use.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(PrecomputeScoresCommand.class);

  /** Configuration parsed from command line. */
  private PrecomputeScoresOptions options;

  /** The HPO ontology. */
  private HpoOntology ontology;

  /** The phenotypic abnormality sub ontology. */
  private ImmutableOntology<HpoTerm, HpoTermRelation> phenotypicAbnormalitySubOntology;

  /** The object ID to TermId mapping. */
  private TreeMap<Integer, Collection<TermId>> objectIdToTermId = new TreeMap<>();

  /** The TermId to object ID mapping. */
  private HashMap<TermId, Collection<Integer>> termIdToObjectId = new HashMap<>();

  /** The Resnik similarity. */
  private ResnikSimilarity<HpoTerm, HpoTermRelation> resnikSimilarity;

  /**
   * The resulting score distribution; will be written out.
   *
   * <p>
   * The output is a mapping from term count to {@link ScoreDistribution} which maps Entrez gene ID
   * to empirical p value distribution.
   * </p>
   */
  Map<Integer, ScoreDistribution> scoreDistribution;

  /** Constructor. */
  public PrecomputeScoresCommand(PrecomputeScoresOptions options) {
    this.options = options;
  }

  /** Execute the command. */
  public void run() {
    printHeader();
    loadOntology();
    precomputePairwiseResnik();
    performSampling();
    writeDistribution();
    printFooter();
  }

  private void printHeader() {
    LOGGER.info("OntoLib CLI -- Precomputing Scores");
    LOGGER.info("");
    LOGGER.info("Options");
    LOGGER.info("=======");
    LOGGER.info("");
    LOGGER.info(options.toString());
  }

  private void loadOntology() {
    LOGGER.info("Loading ontology from OBO...");
    HpoOboParser hpoOboParser = new HpoOboParser(new File(options.getOboFile()));
    try {
      ontology = hpoOboParser.parse();
      phenotypicAbnormalitySubOntology = ontology.getPhenotypicAbnormalitySubOntology();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    LOGGER.info("Done loading ontology.");

    LOGGER.info("Loading gene-to-term link file...");
    final ArrayList<HpoGeneAnnotation> termAnnotations = new ArrayList<>();
    final File inputFile = new File(options.getGeneToTermLinkFile());
    try (HpoGeneAnnotationParser parser = new HpoGeneAnnotationParser(inputFile)) {
      while (parser.hasNext()) {
        final HpoGeneAnnotation anno = parser.next();
        if (phenotypicAbnormalitySubOntology.getNonObsoleteTermIds().contains(anno.getTermId())) {
          termAnnotations.add(anno);
        }
      }
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
      LOGGER.error("Problem reading from file.");
    } catch (TermAnnotationParserException e) {
      e.printStackTrace();
      LOGGER.error(e.getMessage(), e);
      LOGGER.error("Problem reading from file.");
    }

    TermAnnotations
        .constructTermLabelToAnnotationsMap(phenotypicAbnormalitySubOntology, termAnnotations)
        .forEach((geneId, termIds) -> {
          objectIdToTermId.put(Integer.parseInt(geneId.substring("ENTREZ:".length())), termIds);
        });
    TermAnnotations
        .constructTermAnnotationToLabelsMap(phenotypicAbnormalitySubOntology, termAnnotations)
        .forEach((termId, geneIds) -> {
          termIdToObjectId.put(termId,
              geneIds.stream().map(geneId -> Integer.parseInt(geneId.substring("ENTREZ:".length())))
                  .collect(Collectors.toList()));
        });

    LOGGER.info("Done loading gene-phenotype links.");
  }

  private void precomputePairwiseResnik() {
    LOGGER.info("Performing information content precomputation...");
    final InformationContentComputation<HpoTerm, HpoTermRelation> icPrecomputation =
        new InformationContentComputation<>(phenotypicAbnormalitySubOntology);
    final Map<TermId, Double> termToIc =
        icPrecomputation.computeInformationContent(termIdToObjectId);
    LOGGER.info("Done with precomputing information content.");

    LOGGER.info("Performing pairwise Resnik similarity precomputation...");
    resnikSimilarity = new ResnikSimilarity<HpoTerm, HpoTermRelation>(
        phenotypicAbnormalitySubOntology, termToIc, /* symmetric= */false);
    LOGGER.info("Done with precomputing pairwise Resnik similarity.");
  }

  private void performSampling() {
    LOGGER.info("Performing sampling...");

    final ScoreSamplingOptions samplingOptions = new ScoreSamplingOptions(options.getNumThreads(),
        options.getMinObjectId(), options.getMaxObjectId(), options.getMinNumTerms(),
        options.getMaxNumTerms(), options.getSeed(), options.getNumIterations());

    final SimilarityScoreSampling<HpoTerm, HpoTermRelation> sampling =
        new SimilarityScoreSampling<>(phenotypicAbnormalitySubOntology, resnikSimilarity,
            samplingOptions);
    scoreDistribution = sampling.performSampling(objectIdToTermId);

    LOGGER.info("Done with sampling.");
  }

  private void writeDistribution() {
    LOGGER.info("Writing out score distribution...");

    final int resolution = Math.min(1000, Math.max(100, options.getNumIterations() / 100));

    try (final ScoreDistributionWriter writer =
        new TextFileScoreDistributionWriter(new File(options.getOutputScoreDistFile()))) {
      for (Entry<Integer, ScoreDistribution> e : scoreDistribution.entrySet()) {
        writer.write(e.getKey(), e.getValue(), resolution);
      }
    } catch (IOException | OntoLibException e) {
      throw new RuntimeException("Problem writing to file", e);
    }

    LOGGER.info("Done writing out distribution.");
  }

  private void printFooter() {
    LOGGER.info("All Done.\nHave a nice day!\n");
  }

}
