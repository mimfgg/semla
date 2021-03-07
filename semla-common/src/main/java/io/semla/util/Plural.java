package io.semla.util;

import org.atteo.evo.inflector.English;

import java.util.LinkedHashMap;
import java.util.Map;

public class Plural {

    private Plural() {}

    public static String of(String singular) {
        return IRREGULARS.computeIfAbsent(singular, English::plural);
    }

    private static final Map<String, String> IRREGULARS = new LinkedHashMap<String, String>() {{
        put("addendum", "addenda");
        put("alga", "algae");
        put("alumna", "alumnae");
        put("alumnus", "alumni");
        put("analysis", "analyses");
        put("antenna", "antennas");
        put("apparatus", "apparatuses");
        put("appendix", "appendixes");
        put("axis", "axes");
        put("bacillus", "bacilli");
        put("bacterium", "bacteria");
        put("basis", "bases");
        put("beau", "beaux");
        put("bison", "bison");
        put("buffalo", "buffaloes");
        put("bureau", "bureaus");
        put("bus", "bussesbuses");
        put("cactus", "cactuses");
        put("calf", "calves");
        put("child", "children");
        put("corps", "corps");
        put("corpus", "corpuses");
        put("crisis", "crises");
        put("criterion", "criteria");
        put("curriculum", "curricula");
        put("datum", "data");
        put("deer", "deer");
        put("die", "dice");
        put("dwarf", "dwarves");
        put("diagnosis", "diagnoses");
        put("echo", "echoes");
        put("elf", "elves");
        put("ellipsis", "ellipses");
        put("embargo", "embargoes");
        put("emphasis", "emphases");
        put("erratum", "errata");
        put("fireman", "firemen");
        put("fish", "fishfishes");
        put("focus", "focuses");
        put("foot", "feet");
        put("formula", "formulas");
        put("fungus", "funguses");
        put("genus", "genera");
        put("goose", "geese");
        put("half", "halves");
        put("hero", "heroes");
        put("hippopotamus", "hippopotamuses");
        put("hoof", "hooves");
        put("hypothesis", "hypotheses");
        put("index", "indexes");
        put("knife", "knives");
        put("leaf", "leaves");
        put("life", "lives");
        put("loaf", "loaves");
        put("louse", "lice");
        put("man", "men");
        put("matrix", "matrices");
        put("means", "means");
        put("medium", "media");
        put("memorandum", "memoranda");
        put("millennium", "milennia");
        put("moose", "moose");
        put("mosquito", "mosquitoes");
        put("mouse", "mice");
        put("nebula", "nebulas");
        put("neurosis", "neuroses");
        put("nucleus", "nuclei");
        put("oasis", "oases");
        put("octopus", "octopuses");
        put("ovum", "ova");
        put("ox", "oxen");
        put("paralysis", "paralyses");
        put("parenthesis", "parentheses");
        put("person", "people");
        put("phenomenon", "phenomena");
        put("potato", "potatoes");
        put("radius", "radiuses");
        put("scarf", "scarves");
        put("self", "selves");
        put("series", "series");
        put("sheep", "sheep");
        put("shelf", "shelves");
        put("scissors", "scissors");
        put("species", "species");
        put("stimulus", "stimuli");
        put("stratum", "strata");
        put("syllabus", "syllabuses");
        put("symposium", "symposiums");
        put("synthesis", "syntheses");
        put("synopsis", "synopses");
        put("tableau", "tableaux");
        put("that", "those");
        put("thesis", "theses");
        put("thief", "thieves");
        put("this", "these");
        put("tomato", "tomatoes");
        put("tooth", "teeth");
        put("torpedo", "torpedoes");
        put("vertebra", "vertebrae");
        put("veto", "vetoes");
        put("vita", "vitae");
        put("watch", "watches");
        put("wife", "wives");
        put("wolf", "wolves");
        put("woman", "women");
        put("zero", "zeroes");
    }};

}
