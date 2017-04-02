/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.mqm.clt.model;

import java.util.List;

final public class TestRun {

    final private Integer id;
    final private String name;
    final private Release release;

    final private List<Taxonomy> taxonomies;

    public TestRun(Integer id, String name, Release release, List<Taxonomy> taxonomies) {
        this.id = id;
        this.name = name;
        this.release = release;
        this.taxonomies = taxonomies;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Taxonomy> getTaxonomies() {
        return taxonomies;
    }

    public Release getRelease() {
        return release;
    }
}
