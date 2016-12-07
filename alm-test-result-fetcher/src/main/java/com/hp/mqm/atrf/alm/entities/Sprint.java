package com.hp.mqm.atrf.alm.entities;

import com.hp.mqm.atrf.alm.core.AlmEntity;

/**
 * Created by berkovir on 29/06/2016.
 */
public class Sprint extends AlmEntity {

    public static String TYPE = "release-cycle";

    public static String COLLECTION_NAME = "release-cycles";

    public static String FIELD_RELEASE_ID = "parent-id";

    public Sprint() {
        super(TYPE);
    }

    public String getReleaseId() {
        return getString(FIELD_RELEASE_ID);
    }

}