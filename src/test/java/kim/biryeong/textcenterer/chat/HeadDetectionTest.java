package kim.biryeong.textcenterer.chat;

import java.util.List;
import java.util.UUID;

public final class HeadDetectionTest {
    public static void main(String[] args) {
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        UUID kkwi = UUID.randomUUID();
        var serverPlayers = List.of(new HeadDetection.Candidate(kkwi, "kkwi", null),
            new HeadDetection.Candidate(bob, "Bob", null));
        UUID rebuiltSender = HeadDetection.infer("kkwi > 왜 이섭에선 챗헤드가 안될까", serverPlayers);
        check(kkwi, rebuiltSender);
        check(kkwi, new HeadDetection.Result(null, rebuiltSender).select(HeadDetection.Mode.BOTH));
        check(kkwi, new HeadDetection.Result(null, rebuiltSender).select(HeadDetection.Mode.HEURISTIC_ONLY));
        check(null, new HeadDetection.Result(null, rebuiltSender).select(HeadDetection.Mode.UUID_ONLY));
        check(kkwi, HeadDetection.infer("§akkwi §7> Bob > hello", serverPlayers));
        check(kkwi, HeadDetection.infer("[VIP] kkwi > hello", serverPlayers));
        check(null, HeadDetection.infer("Notice: kkwi > hello", serverPlayers));
        check(null, HeadDetection.infer("kkwi2 > hello", serverPlayers));
        check(null, HeadDetection.infer("Unknown > kkwi > hello", serverPlayers));
        var players = List.of(new HeadDetection.Candidate(alice, "Alice", "[VIP] 별명"),
            new HeadDetection.Candidate(bob, "Bob", null));
        check(alice, HeadDetection.infer("<Alice> hello Bob", players));
        check(alice, HeadDetection.infer("[VIP] Alice: hi", players));
        check(alice, HeadDetection.infer("§a[VIP] 별명 » hi", players));
        check(bob, HeadDetection.infer("* Bob waves", players));
        check(null, HeadDetection.infer("Server: Alice joined", players));
        check(null, HeadDetection.infer("<Alice2> hello", players));
        check(null, HeadDetection.infer("Notice mentions Alice: hi", players));
        check(null, HeadDetection.infer("Bob joined the game", players));
        check(null, HeadDetection.infer("<Alice> hi", List.of(
            new HeadDetection.Candidate(alice, "Alice", null),
            new HeadDetection.Candidate(bob, "Bob", "Alice"))));
        var conflict = new HeadDetection.Result(alice, bob);
        check(alice, conflict.select(HeadDetection.Mode.UUID_ONLY));
        check(bob, conflict.select(HeadDetection.Mode.HEURISTIC_ONLY));
        check(alice, conflict.select(HeadDetection.Mode.BOTH));
        var unsigned = new HeadDetection.Result(null, bob);
        check(null, unsigned.select(HeadDetection.Mode.UUID_ONLY));
        check(bob, unsigned.select(HeadDetection.Mode.BOTH));
        check(null, new HeadDetection.Result(alice, null).select(HeadDetection.Mode.HEURISTIC_ONLY));
        check(HeadDetection.Mode.BOTH, HeadDetection.Mode.parse("invalid"));
        check(HeadDetection.Mode.BOTH, HeadDetection.Mode.BOTH.next().next().next());
        System.out.println("Head detection checks passed");
    }
    private static void check(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
    }
}
