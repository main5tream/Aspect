import java.util.ArrayList;
import java.util.List;


public class Mapping {
	
	static interface Function<IN,OUT> { 
		OUT apply(IN arg); 
	}

	public static <T, U> List<U> map(Function<T, U> f, List<T> l){
		List<U> result = new ArrayList<U>(l.size());
		for(T element:l){
			result.add(f.apply(element));
		}
		return result; 
	}

	public static void main(String[] args) {
		List<Number> three = new ArrayList<Number>();
		three.add(1);
		three.add(2);
		three.add(3);

		Function<Number,Number> plusOne = new Function<Number, Number>() { 
			@Override
			public Number apply(Number arg) { 
				return arg.doubleValue() + 1;
			}
		};

		System.out.println(map(plusOne,three));

	}
}
