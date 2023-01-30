import static java.lang.Math.ceil;
import static java.lang.Math.max;

/**
 * 
 * @author CD4017BE */
public class Recipe {

	/** Recipe name including prefix */
	public final String name;
	/** Whether this recipe can only run in integer steps */
	public final boolean intCount;
	/** How often this recipe has been run */
	public double count;
	/** List of materials (ingredients and products) */
	public Ingredient[] ioMaterials;
	/** Amount in which each element of {@link #ioMaterials} appears (product > 0, ingredient < 0) */
	public double[] ioCounts;

	public Recipe(String name) {
		this.name = name;
		this.intCount = name.charAt(0) == '%';
	}

	public void init(Ingredient[] ioMaterials, double[] ioCounts) {
		this.ioMaterials = ioMaterials;
		this.ioCounts = ioCounts;
		Ingredient ingr;
		for (int i = 0; i < ioCounts.length; i++)
			if (ioCounts[i] > 0 && ((ingr = ioMaterials[i]).recipe == null || ingr.recipe == this)) {
				ingr.recipe = this;
				double scale = 0;
				for (int j = 0; j < ioMaterials.length; j++)
					if (ioMaterials[j] == ingr)
						scale += ioCounts[j];
				ingr.scale = -1.0 / scale;
			}
	}

	public Ingredient run(Ingredient update) {
		double count = -this.count;
		for (Ingredient ingr : ioMaterials)
			if (ingr.recipe == this) {
				double x = ingr.remainder * ingr.scale;
				count = max(count, intCount ? ceil(x) : x);
			}
		if (count >= -Main.EPSILON && count <= Main.EPSILON)
			return update;
		this.count += count;
		for (int i = 0; i < ioCounts.length; i++)
			update = ioMaterials[i].add(count * ioCounts[i], update);
		return update;
	}

	@Override
	public String toString() {
		String s = Main.FORMAT.formatted(count, name);
		if (!Main.LIST_INGRED) return s;
		StringBuilder sb = new StringBuilder(s);
		sb.append(": ");
		for (int i = 0; i < ioCounts.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(Main.FORMAT.formatted(ioCounts[i] * count, ioMaterials[i].name));
		}
		return sb.toString();
	}

}
