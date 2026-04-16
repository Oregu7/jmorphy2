package company.evo.jmorphy2.elasticsearch.index;

import com.carrotsearch.randomizedtesting.ThreadFilter;

public class ForkJoinPoolThreadFilter implements ThreadFilter {
    @Override
    public boolean reject(Thread t) {
        return t.getName().startsWith("ForkJoinPool.");
    }
}
