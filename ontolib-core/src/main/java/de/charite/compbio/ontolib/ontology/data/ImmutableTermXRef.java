package de.charite.compbio.ontolib.ontology.data;

/**
 * Immutable implementation of {@link TermXRef}.
 *
 * @author <a href="mailto:manuel.holtgrewe@bihealth.de">Manuel Holtgrewe</a>
 */
public class ImmutableTermXRef implements TermXRef {

  /** Serial UId for serialization. */
  private static final long serialVersionUID = 1L;

  /** Referenced term Id. */
  private final TermId id;

  /** Referenced description. */
  private final String description;

  /**
   * Constructor.
   *
   * @param id The term's Id.
   * @param description The cross reference description.
   */
  public ImmutableTermXRef(TermId id, String description) {
    this.id = id;
    this.description = description;
  }

  @Override
  public TermId getId() {
    return id;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return "ImmutableTermXRef [id=" + id + ", description=" + description + "]";
  }
  
}
