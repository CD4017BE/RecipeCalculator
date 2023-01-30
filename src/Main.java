import java.io.*;
import java.util.*;
import java.util.Locale.Category;

/**
 * 
 * @author CD4017BE */
public class Main {

	static final HashMap<String, Ingredient> ingredients = new HashMap<>();
	static final HashMap<String, Recipe> recipes = new HashMap<>();
	static String FORMAT = "%.5g %s";
	static int LIMIT = 100000;
	static double EPSILON = 1e-12;
	static boolean LIST_INGRED = false;
	record Element(double count, String name) {}

	public static void main(String[] args) throws IOException {
		Locale.setDefault(Category.FORMAT, Locale.ROOT);
		String input = null, output = null;
		for (String arg : args) {
			if (arg.length() >= 2 && arg.charAt(0) == '-')
				try {
					String s = arg.substring(2);
					switch(arg.charAt(1)) {
					case 'p':
						FORMAT = "%." + Integer.parseInt(s) + "g %s\n";
						break;
					case 'l':
						LIMIT = Integer.parseInt(s);
						break;
					case 'e':
						EPSILON = Math.abs(Double.parseDouble(s));
						break;
					case 'i':
						LIST_INGRED = true;
						break;
					default: throw new NumberFormatException();
					}
				} catch(NumberFormatException e) {
					error("invalid argument " + arg);
				}
			else if (input == null) input = arg;
			else if (output == null) output = arg;
			else error("too many arguments");
		}
		if (input == null) error("missing input file argument");
		Ingredient first = new Ingredient(null);
		Ingredient last = load(new File(input), first);
		boolean success = update(first, last);
		printResult(output, success);
	}

	private static void error(String msg) {
		System.out.println(msg);
		System.out.print(
			"\nUsage: [options...] input_file [output_file]\n"
			+ "Possible options:\n"
			+ " -p<number>  sets the number of significant digits to print in output (default: -p5).\n"
			+ " -l<number>  sets the maximum number of recipe evaluations to do before giving up (default: -l100000).\n"
			+ " -e<number>  sets the roundoff limit that is considered equal to zero (default: -e1E-12).\n"
			+ " -i          makes result also list the ingredients of each recipe.\n"
			+ "If output_file is given then the calculated results are written there, otherwise they are written to standard output.\n"
			+ "Each line in input_file may contain recipe or ingredient specifications:\n"
			+ " <name>: <amount>, $<recipe>   specifies the ingredient <name> with optionally an initial <amount> and/or <recipe>. Ingredients with negative initial amount are meant to be crafted.\n"
			+ " $<name>: <amount> <ingredient>, ...   specifies the recipe <name> as a list of <ingredient> names with <amount>s. Negative amounts are inputs and positive amounts are outputs.\n"
			+ " %<name>: <amount> <ingredient>, ...   same as above but for recipes that can only be crafted in integer amounts.\n"
			+ "Lines containing no ':' or starting with '#' are ignored.\n"
			+ "Ingredient names that appear in recipes without being explicitly defined will be implicitly defined with default recipe and initial amount 0. Ingredients with no recipe specified will use the first recipe that has them as output.\n"
			+ "A line starting with '!' followed by a file path will include the contents of that file for parsing recipes and ingredients.\n"
		);
		System.exit(1);
	}

	private static Ingredient load(File src, Ingredient update) throws IOException {
		ArrayList<Element> list = new ArrayList<>();
		try(BufferedReader r = new BufferedReader(new FileReader(src))) {
			for (String l; (l = r.readLine()) != null;) {
				if (l.isEmpty()) continue;
				char c = l.charAt(0);
				if (c == '#') continue;
				if (c == '!') {
					update = load(new File(src.getParentFile(), l.substring(1).trim()), update);
					continue;
				}
				int p = l.indexOf(':');
				if (p < 0) continue;
				String name = l.substring(0, p++).trim();
				for (int q; (q = l.indexOf(',', p)) >= 0; p = q + 1)
					parse(l, p, q, list);
				parse(l, p, l.length(), list);
				if (!name.isEmpty() && (c = name.charAt(0)) == '$' || c == '%') {
					//recipe
					int i = 0, n = list.size();
					Recipe recipe = recipe(name);
					if (n > 0 && list.get(i).name.equals("#"))
						recipe.count = list.get(i++).count;
					double[] counts = new double[n - i];
					Ingredient[] ingreds = new Ingredient[n - i];
					for (int j = 0; i < n; i++, j++) {
						Element e = list.get(i);
						double count = counts[j] = e.count;
						if (count == 0) throw new IOException("missing ingredient count in '" + l + "'");
						ingreds[j] = ingredient(e.name);
					}
					recipe.init(ingreds, counts);
				} else {
					//ingredient
					Ingredient ingr = ingredient(name);
					for (Element e : list) {
						if (e.name.isEmpty())
							update = ingr.add(e.count, update);
						else ingr.recipe = recipe(e.name);
					}
				}
				list.clear();
			}
		}
		return update;
	}

	private static void parse(String s, int i, int j, List<Element> list) {
		while(i < j && Character.isWhitespace(s.charAt(i))) i++;
		if (i >= j) return;
		char c = s.charAt(i);
		double count = 0.0;
		if (c == '+' || c == '-' || c >= '0' && c <= '9') {
			int i0 = i;
			for (i++; i < j; i++)
				if (Character.isWhitespace(s.charAt(i))) break;
			count = Double.parseDouble(s.substring(i0, i));
		}
		list.add(new Element(count, s.substring(i, j).trim()));
	}

	private static Recipe recipe(String name) {
		return recipes.computeIfAbsent(name, Recipe::new);
	}

	private static Ingredient ingredient(String name) {
		return ingredients.computeIfAbsent(name, Ingredient::new);
	}

	private static boolean update(Ingredient first, Ingredient last) {
		int i = LIMIT;
		while ((first = first.next()) != null && i-- > 0)
			last = first.update(last);
		return i > 0;
	}

	private static void printResult(String output, boolean success) throws IOException {
		ArrayList<Ingredient> ingredients = new ArrayList<>();
		ArrayList<Recipe> recipes = new ArrayList<>();
		for (Ingredient ingr : Main.ingredients.values())
			if (ingr.hasRemainder())
				ingredients.add(ingr);
		boolean fin = true;
		for (Recipe recipe : Main.recipes.values())
			if (recipe.count > EPSILON) {
				fin &= Double.isFinite(recipe.count);
				recipes.add(recipe);
			}
		Collections.sort(ingredients, (a, b) -> a.name.compareTo(b.name));
		Collections.sort(recipes, (a, b) -> a.name.compareTo(b.name));
		try (
			PrintStream out = output == null ? System.out
				: new PrintStream(new FileOutputStream(new File(output)))
		) {
			if (!fin) out.print("Solution diverged, possibly due to a recursive recipe loop producing net loss!\n");
			else if (success) out.print("Solution converged successfully.\n");
			else out.printf("Solution didn't converge below +/- %.2g within %d iteration steps!\n", EPSILON, LIMIT);
			
			out.print("Remaining items:\n");
			for (Ingredient ingr : ingredients) out.println(ingr);
			out.print("Used recipes:\n");
			for (Recipe recipe : recipes) out.println(recipe);
		}
	}

}
