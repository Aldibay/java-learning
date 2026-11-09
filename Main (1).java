// Бір файлдық нұсқа (Main.java)
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

// === Main.java ===
public class Main {
    public static void main(String[] args) {
        System.out.println("=== " + ClubSettings.getInstance().getClubName() + " ===");

        MemberRepository memberRepository = new InMemoryMemberRepository();
        PurchaseRepository purchaseRepository = new InMemoryPurchaseRepository();
        TrainingRepository trainingRepository = new InMemoryTrainingRepository();

        MembershipEventBus eventBus = new MembershipEventBus();
        MembershipStatisticsObserver statistics = new MembershipStatisticsObserver();
        eventBus.subscribe(new ConsoleNotificationObserver());
        eventBus.subscribe(statistics);

        MemberService memberService = new MemberService(memberRepository);
        MembershipService membershipService = new MembershipService(
                memberRepository, purchaseRepository, eventBus);
        TrainingService trainingService = new TrainingService(trainingRepository);
        ClubReport report = new ClubReport(memberRepository, purchaseRepository, trainingRepository);

        Member member1 = new Member.Builder("M101", "Әлихан").email("alikhan@example.com").build();
        Member member2 = new Member.Builder("M102", "Айдана").email("aidana@example.com").build();
        memberService.register(member1);
        memberService.register(member2);
        System.out.println("Тіркелгендер: " + member1.getName() + ", " + member2.getName());

        Coach coach = new Coach("C201", "Ерлан", "Жүзу");
        System.out.println("Қызметкерлер мен мүшелер (Person арқылы):");
        Person[] people = {member1, member2, coach};
        for (Person person : people) {
            System.out.println(" - " + person.getName() + " (" + person.getRole() + ")");
        }

        PlanFactory planFactory = new PlanFactory();
        planFactory.register("MONTH", () -> new MonthlyPlan());
        planFactory.register("QUARTER", () -> new QuarterlyPlan());
        MembershipPlan monthly = planFactory.create("MONTH");
        MembershipPlan quarterly = planFactory.create("QUARTER");

        MembershipPurchase firstSale = membershipService.purchase(
                "M101", monthly, new StudentPricing());
        MembershipPurchase secondSale = membershipService.purchase(
                "M102", quarterly, new RegularPricing());
        System.out.println("Төленді: " + firstSale.getFinalPrice() + " ₸ және "
                + secondSale.getFinalPrice() + " ₸");

        TrainingSession swimming = new TrainingSession("T301", "Жүзу сабағы", coach, 2);
        trainingService.schedule(swimming);
        trainingService.enroll("T301", member1);
        trainingService.enroll("T301", member2);
        System.out.println("Жаттығуға тіркелгендер саны: " + swimming.getAttendees().size());
        report.print();
        System.out.println("Observer статистикасы: сатып алу саны = "
                + statistics.getPurchaseCount() + ", жалпы кіріс = "
                + statistics.getTotalIncome() + " ₸");
        System.out.println("Бағдарлама сәтті аяқталды.");
    }
}

// === config/ClubSettings.java ===
// Singleton: қосымшада бір конфигурация объектісі ғана бар.
final class ClubSettings {
    private static final ClubSettings INSTANCE = new ClubSettings();
    private final String clubName = "SPORT CLUB — басқару жүйесі";

    private ClubSettings() { }

    public static ClubSettings getInstance() { return INSTANCE; }
    public String getClubName() { return clubName; }
}

// === factory/PlanFactory.java ===
// Factory + OCP: жаңа тарифті register() арқылы қосуға болады.
final class PlanFactory {
    private final Map<String, Supplier<MembershipPlan>> creators = new HashMap<>();

    public void register(String code, Supplier<MembershipPlan> creator) {
        if (code == null || code.isBlank() || creator == null) {
            throw new IllegalArgumentException("Тариф тіркеу деректері жарамсыз");
        }
        creators.put(code, creator);
    }

    public MembershipPlan create(String code) {
        Supplier<MembershipPlan> creator = creators.get(code);
        if (creator == null) throw new IllegalArgumentException("Белгісіз тариф: " + code);
        return creator.get();
    }
}

// === model/Coach.java ===
final class Coach extends Person {
    private final String specialization;

    public Coach(String id, String name, String specialization) {
        super(id, name);
        if (specialization == null || specialization.isBlank()) {
            throw new IllegalArgumentException("Мамандық бос болмауы керек");
        }
        this.specialization = specialization;
    }

    public String getSpecialization() { return specialization; }
    @Override public String getRole() { return "Жаттықтырушы: " + specialization; }
}

// === model/Member.java ===
// Builder: міндетті және қосымша деректерді ыңғайлы орнату.
final class Member extends Person {
    private final String email;

    private Member(Builder builder) {
        super(builder.id, builder.name);
        this.email = builder.email;
    }

    public String getEmail() { return email; }
    @Override public String getRole() { return "Клуб мүшесі"; }

    public static final class Builder {
        private final String id;
        private final String name;
        private String email = "Көрсетілмеген";

        public Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder email(String email) {
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Email бос болмауы керек");
            }
            this.email = email;
            return this;
        }

        public Member build() { return new Member(this); }
    }
}

// === model/MembershipPlan.java ===
// LSP: нақты тарифтер ата-аналық типтің келісімін сақтайды.
abstract class MembershipPlan {
    private final String name;
    private final int months;
    private final BigDecimal basePrice;

    protected MembershipPlan(String name, int months, BigDecimal basePrice) {
        if (name == null || name.isBlank() || months <= 0 || basePrice == null
                || basePrice.signum() < 0) {
            throw new IllegalArgumentException("Тариф деректері дұрыс емес");
        }
        this.name = name;
        this.months = months;
        this.basePrice = basePrice;
    }

    public String getName() { return name; }
    public int getMonths() { return months; }
    public BigDecimal getBasePrice() { return basePrice; }
}

// === model/MembershipPurchase.java ===
// Тек сатып алу деректерін сақтайды (SRP).
final class MembershipPurchase {
    private final Member member;
    private final MembershipPlan plan;
    private final BigDecimal finalPrice;
    private final String pricingName;

    public MembershipPurchase(Member member, MembershipPlan plan,
                              BigDecimal finalPrice, String pricingName) {
        if (member == null || plan == null || finalPrice == null
                || finalPrice.signum() < 0 || pricingName == null || pricingName.isBlank()) {
            throw new IllegalArgumentException("Сатып алу деректері қате");
        }
        this.member = member;
        this.plan = plan;
        this.finalPrice = finalPrice;
        this.pricingName = pricingName;
    }

    public Member getMember() { return member; }
    public MembershipPlan getPlan() { return plan; }
    public BigDecimal getFinalPrice() { return finalPrice; }
    public String getPricingName() { return pricingName; }
}

// === model/MonthlyPlan.java ===
final class MonthlyPlan extends MembershipPlan {
    public MonthlyPlan() { super("Айлық абонемент", 1, new BigDecimal("12000.00")); }
}

// === model/Person.java ===
// LSP: Member және Coach осы базалық типтің орнына қолданылады.
abstract class Person {
    private final String id;
    private final String name;

    protected Person(String id, String name) {
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("ID және аты бос болмауы керек");
        }
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public abstract String getRole();
}

// === model/QuarterlyPlan.java ===
final class QuarterlyPlan extends MembershipPlan {
    public QuarterlyPlan() { super("Үш айлық абонемент", 3, new BigDecimal("30000.00")); }
}

// === model/TrainingSession.java ===
// Композиция/агрегация: сабақта Coach және Member объектілеріне сілтемелер сақталады.
final class TrainingSession {
    private final String id;
    private final String title;
    private final Coach coach;
    private final int capacity;
    private final List<Member> attendees = new ArrayList<>();

    public TrainingSession(String id, String title, Coach coach, int capacity) {
        if (id == null || id.isBlank() || title == null || title.isBlank()
                || coach == null || capacity <= 0) {
            throw new IllegalArgumentException("Сабақ мәліметтері дұрыс емес");
        }
        this.id = id;
        this.title = title;
        this.coach = coach;
        this.capacity = capacity;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public Coach getCoach() { return coach; }
    public int getCapacity() { return capacity; }
    public List<Member> getAttendees() {
        return Collections.unmodifiableList(attendees);
    }

    public void addMember(Member member) {
        if (member == null) throw new IllegalArgumentException("Мүше көрсетілмеген");
        if (attendees.stream().anyMatch(m -> m.getId().equals(member.getId()))) {
            throw new IllegalArgumentException("Мүше бұл сабаққа бұрын жазылған");
        }
        if (attendees.size() >= capacity) {
            throw new IllegalStateException("Сабақтағы орындар толды");
        }
        attendees.add(member);
    }
}

// === observer/ConsoleNotificationObserver.java ===
// Демо: нақты email/SMS жібермейді; тек консольге басады.
final class ConsoleNotificationObserver implements MembershipObserver {
    @Override public void onPurchase(MembershipPurchase purchase) {
        System.out.println("[Хабарлама, демо] " + purchase.getMember().getName()
                + " үшін «" + purchase.getPlan().getName() + "» қосылды.");
    }
}

// === observer/MembershipEventBus.java ===
// Observer: тіркелген бақылаушыларға хабар жібереді.
final class MembershipEventBus implements MembershipPublisher {
    private final List<MembershipObserver> observers = new ArrayList<>();

    public void subscribe(MembershipObserver observer) {
        if (observer == null) throw new IllegalArgumentException("Бақылаушы бос");
        observers.add(observer);
    }

    @Override public void publish(MembershipPurchase purchase) {
        for (MembershipObserver observer : observers) observer.onPurchase(purchase);
    }
}

// === observer/MembershipObserver.java ===
// ISP: бақылаушыға тек бір оқиға туралы хабар беріледі.
interface MembershipObserver {
    void onPurchase(MembershipPurchase purchase);
}

// === observer/MembershipPublisher.java ===
// DIP: сервис нақты EventBus класын білмейді.
interface MembershipPublisher {
    void publish(MembershipPurchase purchase);
}

// === observer/MembershipStatisticsObserver.java ===
final class MembershipStatisticsObserver implements MembershipObserver {
    private int purchaseCount = 0;
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @Override public void onPurchase(MembershipPurchase purchase) {
        purchaseCount++;
        totalIncome = totalIncome.add(purchase.getFinalPrice());
    }

    public int getPurchaseCount() { return purchaseCount; }
    public BigDecimal getTotalIncome() { return totalIncome; }
}

// === pricing/PricingStrategy.java ===
// ISP: тек бағаны есептеуге қажетті шағын интерфейс.
interface PricingStrategy {
    BigDecimal calculate(BigDecimal basePrice);
    String getName();
}

// === pricing/RegularPricing.java ===
final class RegularPricing implements PricingStrategy {
    @Override public BigDecimal calculate(BigDecimal basePrice) {
        return basePrice.setScale(2, RoundingMode.HALF_UP);
    }
    @Override public String getName() { return "Жеңілдіксіз баға"; }
}

// === pricing/StudentPricing.java ===
// Strategy: студент үшін 10% жеңілдік; қызмет коды өзгермейді.
final class StudentPricing implements PricingStrategy {
    @Override public BigDecimal calculate(BigDecimal basePrice) {
        return basePrice.multiply(new BigDecimal("0.90"))
                .setScale(2, RoundingMode.HALF_UP);
    }
    @Override public String getName() { return "Студенттік жеңілдік 10%"; }
}

// === repository/InMemoryMemberRepository.java ===
final class InMemoryMemberRepository implements MemberRepository {
    private final Map<String, Member> members = new LinkedHashMap<>();

    @Override public void save(Member member) {
        if (members.containsKey(member.getId())) {
            throw new IllegalArgumentException("Мүше ID қайталанды: " + member.getId());
        }
        members.put(member.getId(), member);
    }

    @Override public Optional<Member> findById(String id) {
        return Optional.ofNullable(members.get(id));
    }

    @Override public List<Member> findAll() {
        return new ArrayList<>(members.values());
    }
}

// === repository/InMemoryPurchaseRepository.java ===
final class InMemoryPurchaseRepository implements PurchaseRepository {
    private final List<MembershipPurchase> purchases = new ArrayList<>();

    @Override public void save(MembershipPurchase purchase) { purchases.add(purchase); }
    @Override public List<MembershipPurchase> findAll() {
        return new ArrayList<>(purchases);
    }
}

// === repository/InMemoryTrainingRepository.java ===
final class InMemoryTrainingRepository implements TrainingRepository {
    private final Map<String, TrainingSession> sessions = new LinkedHashMap<>();

    @Override public void save(TrainingSession session) {
        if (sessions.containsKey(session.getId())) {
            throw new IllegalArgumentException("Сабақ ID қайталанды: " + session.getId());
        }
        sessions.put(session.getId(), session);
    }

    @Override public Optional<TrainingSession> findById(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    @Override public List<TrainingSession> findAll() {
        return new ArrayList<>(sessions.values());
    }
}

// === repository/MemberRepository.java ===
// DIP: сақтау тәсілі интерфейстің артында жасырылған.
interface MemberRepository {
    void save(Member member);
    Optional<Member> findById(String id);
    List<Member> findAll();
}

// === repository/PurchaseRepository.java ===
interface PurchaseRepository {
    void save(MembershipPurchase purchase);
    List<MembershipPurchase> findAll();
}

// === repository/TrainingRepository.java ===
interface TrainingRepository {
    void save(TrainingSession session);
    Optional<TrainingSession> findById(String id);
    List<TrainingSession> findAll();
}

// === service/MemberService.java ===
// SRP + DIP: тек мүшені тіркейді; сақтау интерфейс арқылы.
final class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        if (memberRepository == null) throw new IllegalArgumentException("Репозиторий бос");
        this.memberRepository = memberRepository;
    }

    public void register(Member member) {
        memberRepository.save(member);
    }
}

// === service/MembershipService.java ===
// SRP: абонемент сатып алуды ұйымдастыру.
// DIP: тәуелділіктер конструктор арқылы интерфейс түрінде беріледі.
final class MembershipService {
    private final MemberRepository members;
    private final PurchaseRepository purchases;
    private final MembershipPublisher publisher;

    public MembershipService(MemberRepository members, PurchaseRepository purchases,
                             MembershipPublisher publisher) {
        if (members == null || purchases == null || publisher == null) {
            throw new IllegalArgumentException("Тәуелділіктер бос болмауы тиіс");
        }
        this.members = members;
        this.purchases = purchases;
        this.publisher = publisher;
    }

    public MembershipPurchase purchase(String memberId, MembershipPlan plan,
                                       PricingStrategy pricing) {
        if (plan == null || pricing == null) {
            throw new IllegalArgumentException("Тариф немесе баға стратегиясы жоқ");
        }
        Member member = members.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Мүше табылмады: " + memberId));
        BigDecimal amount = pricing.calculate(plan.getBasePrice());
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Есептелген баға жарамсыз");
        }
        MembershipPurchase purchase = new MembershipPurchase(member, plan, amount,
                pricing.getName());
        purchases.save(purchase);
        publisher.publish(purchase);
        return purchase;
    }
}

// === service/TrainingService.java ===
// SRP: жаттығуды жоспарлау және жазылуды басқару.
final class TrainingService {
    private final TrainingRepository sessions;

    public TrainingService(TrainingRepository sessions) {
        if (sessions == null) throw new IllegalArgumentException("Репозиторий бос");
        this.sessions = sessions;
    }

    public void schedule(TrainingSession session) {
        sessions.save(session);
    }

    public void enroll(String sessionId, Member member) {
        TrainingSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Сабақ табылмады: " + sessionId));
        session.addMember(member);
    }
}

// === view/ClubReport.java ===
// SRP: тек есепті көрсетеді, деректі сақтамайды.
final class ClubReport {
    private final MemberRepository members;
    private final PurchaseRepository purchases;
    private final TrainingRepository trainings;

    public ClubReport(MemberRepository members, PurchaseRepository purchases,
                      TrainingRepository trainings) {
        this.members = members;
        this.purchases = purchases;
        this.trainings = trainings;
    }

    public void print() {
        System.out.println("\n=== СПОРТ КЛУБ ЕСЕБІ ===");
        System.out.println("Мүшелер:");
        for (Member member : members.findAll()) {
            System.out.println(" - " + member.getId() + ": " + member.getName()
                    + ", email: " + member.getEmail());
        }
        BigDecimal total = BigDecimal.ZERO;
        System.out.println("Абонементтер:");
        for (MembershipPurchase purchase : purchases.findAll()) {
            total = total.add(purchase.getFinalPrice());
            System.out.println(" - " + purchase.getMember().getName() + ": "
                    + purchase.getPlan().getName() + ", " + purchase.getFinalPrice() + " ₸");
        }
        System.out.println("Жаттығулар:");
        for (TrainingSession session : trainings.findAll()) {
            System.out.println(" - " + session.getTitle() + ", жаттықтырушы: "
                    + session.getCoach().getName() + ", қатысушылар: "
                    + session.getAttendees().size() + "/" + session.getCapacity());
        }
        System.out.println("Клуб кірісі: " + total + " ₸");
    }
}
