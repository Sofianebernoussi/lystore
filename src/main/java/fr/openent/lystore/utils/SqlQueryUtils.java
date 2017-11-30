package fr.openent.lystore.utils;

import java.util.List;

public class SqlQueryUtils {

    public static StringBuilder prepareMultipleIds (List<Integer> ids) {
        StringBuilder filter = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                filter.append("OR ");
            }
            filter.append("id = ? ");
        }

        return filter;
    }
}
