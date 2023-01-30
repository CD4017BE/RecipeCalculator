

/**
 * 
 * @author CD4017BE */
public class Ingredient {

	/** Material name */
	public final String name;
	/** Recipe used to produce this material */
	public Recipe recipe;
	/** Remaining amount of this material (missing < 0, surplus > 0) */
	public double remainder;
	/** How often {@link #recipe} must run to decrement {@link #remainder} by one */
	public double scale;
	/** Linked list of Ingredients to update */
	public Ingredient next;

	public Ingredient(String name) {
		this.name = name;
	}

	public Ingredient add(double amount, Ingredient update) {
		remainder += amount;
		return next == null && this != update ? update.next = this : update;
	}

	public Ingredient update(Ingredient update) {
		return hasRemainder() && recipe != null ? recipe.run(update) : update;
	}

	public boolean hasRemainder() {
		return remainder < -Main.EPSILON || remainder > Main.EPSILON;
	}

	public Ingredient next() {
		Ingredient next = this.next;
		this.next = null;
		return next;
	}

	@Override
	public String toString() {
		return Main.FORMAT.formatted(remainder, name);
	}

}
