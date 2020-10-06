package io.sapl.generator;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@Data
@Slf4j
@Configuration
public class DomainData {

    @Value("${sapl.policy-directory.path:#systemProperties['\"user.home']+'/policies'}")
    private String policyDirectoryPath;

    @Value("${sapl.policy-directory.clean-on-start:true}")
    private boolean cleanPolicyDirectory;

    @Value("${sapl.random.seed:2454325}")
    private long seed;

    //#### DOMAIN ####
    @Value("${sapl.number.of.subjects}")
    private int numberOfSubjects;
    @Value("${sapl.maximum.of.subject.roles}")
    private int limitOfSubjectRoles;
    @Value("${sapl.number.of.subjects.locked}")
    private int numberOfLockedSubjects;

    //actions
    @Value("${sapl.number.of.actions}")
    private int numberOfActions;

    //resources
    @Value("${sapl.number.of.resources}")
    private int numberOfGeneralResources;
    @Value("${sapl.probability.of.extended.resource}")
    private double probabilityOfExtendedResource;
    @Value("${sapl.probability.of.unrestricted.resource}")
    private double probabilityOfUnrestrictedResource;

    //roles
    @Value("${sapl.number.of.roles}")
    private int numberOfGeneralRoles;
    @Value("${sapl.number.of.roles.per.subject}")
    private int numberOfRolesPerSubject;
    @Value("${sapl.probability.of.extended.role}")
    private double probabilityOfExtendedRole;
    @Value("${sapl.probability.of.full.access.role}")
    private double probabilityOfGeneralFullAccessRole;
    @Value("${sapl.probability.of.read.access.role}")
    private double probabilityOfGeneralReadAccessRole;
    @Value("${sapl.probability.of.custom.access.role}")
    private double probabilityOfGeneralCustomAccessRole;

    //per resource & role
    @Value("${sapl.probability.of.full.access.on.resource}")
    private double probabilityFullAccessOnResource;
    @Value("${sapl.probability.of.read.access.on.resource}")
    private double probabilityReadAccessOnResource;
    @Value("${sapl.probability.of.custom.access.on.resource}")
    private double probabilityCustomAccessOnResource;


    //AuthorizationSubscription Generation
    @Value("${sapl.number.of.benchmark.runs}")
    private int numberOfBenchmarkRuns;

    @Value("${sapl.subscription.generation.factor}")
    private int subscriptionGenerationFactor;
    @Value("${sapl.probability.empty.subscription:0.8}")
    private double probabilityEmptySubscription;
    @Value("${sapl.probability.empty.subscription.node:0.4}")
    private double probabilityEmptySubscriptionNode;

    //there should be more subscriptions than executions of the benchmark to avoid a subscription beeing used twice
    private int numberOfGeneratedSubscriptions = this.numberOfBenchmarkRuns * this.subscriptionGenerationFactor + 100;

    private Random dice;
    private DomainUtil domainUtil;

    private List<DomainRole> domainRoles = new LinkedList<>();
    private List<DomainResource> domainResources = new LinkedList<>();
    private List<DomainSubject> domainSubjects = new LinkedList<>();
    private List<String> domainActions = new LinkedList<>();

    public List<DomainRole> getDomainRoles() {
        return List.copyOf(domainRoles);
    }

    public List<DomainResource> getDomainResources() {
        return List.copyOf(domainResources);
    }

    public List<DomainSubject> getDomainSubjects() {
        return List.copyOf(domainSubjects);
    }

    public List<String> getDomainActions() {
        return List.copyOf(domainActions);
    }


    @Bean
    public Random dice() {
        this.dice = new Random(seed);

        return this.dice;
    }

    public Random initDiceWithSeed(long newSeed) {
        this.seed = newSeed;
        return dice();
    }

    public double roll() {
        return getDice().nextDouble();
    }

    public boolean rollIsLowerThanProbability(double probability) {
        return roll() < probability;
    }

    private <T> T getRandomElement(List<T> list) {
        return list.get(getDice().nextInt(list.size()));
    }

    @Bean
    @DependsOn("dice")
    public DomainUtil generatorUtility() {
        this.domainUtil = new DomainUtil(cleanPolicyDirectory);

        this.domainRoles = generateRoles();
        this.domainResources = generateResources();
        this.domainSubjects = generateSubjects(this.domainRoles);
        this.domainActions = DomainActions.generateActionListByCount(this.getNumberOfActions());

        LOGGER.debug("generated {} roles", this.domainRoles.size());
        LOGGER.debug("generated {} resources", this.domainResources.size());
        LOGGER.debug("generated {} subjects", this.domainSubjects.size());
        LOGGER.debug("generated {} actions", this.domainActions.size());

        return this.domainUtil;
    }

    @DependsOn("generatorUtility")
    private List<DomainRole> generateRoles() {
        List<DomainRole> roles = new ArrayList<>();

        for (int i = 0; i < this.getNumberOfGeneralRoles(); i++) {
            roles.add(new DomainRole(String.format("role.%03d", DomainUtil.getNextRoleCount()),
                    rollIsLowerThanProbability(this.getProbabilityOfGeneralFullAccessRole()),
                    rollIsLowerThanProbability(this.getProbabilityOfGeneralReadAccessRole()),
                    rollIsLowerThanProbability(this.getProbabilityOfGeneralCustomAccessRole()),
                    rollIsLowerThanProbability(this.getProbabilityOfExtendedRole())
            ));
        }
        return roles;
    }

    @DependsOn("generatorUtility")
    private List<DomainResource> generateResources() {
        List<DomainResource> resources = new ArrayList<>();

        for (int i = 0; i < this.getNumberOfGeneralResources(); i++) {
            resources.add(new DomainResource(String.format("resource.%03d", DomainUtil.getNextResourceCount()),
                    rollIsLowerThanProbability(this.getProbabilityOfUnrestrictedResource()),
                    rollIsLowerThanProbability(this.getProbabilityOfExtendedRole())
            ));
        }
        return resources;
    }

    @DependsOn("generatorUtility")
    private List<DomainSubject> generateSubjects(List<DomainRole> allRoles) {
        List<DomainSubject> subjects = new ArrayList<>();

        for (int i = 0; i < this.getNumberOfSubjects(); i++) {
            DomainSubject domainSubject = new DomainSubject(String.format("subject.%03d", i));

            //assign subject random roles
            for (int j = 0; j < this.dice.nextInt(this.getLimitOfSubjectRoles()) + 1; j++) {
                DomainRole randomRole = getRandomElement(allRoles);
                domainSubject.getSubjectAuthorities().add(randomRole.getRoleName());
            }

            subjects.add(domainSubject);
        }

        return subjects;
    }

}
