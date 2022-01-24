package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.common.Internal;

import static java.util.Locale.ROOT;

public @Internal enum Technology {
    GRAPHQL,
    REST;

    /** As it's commonly used in URI paths */
    public String path() {return name().toLowerCase(ROOT);}
}
