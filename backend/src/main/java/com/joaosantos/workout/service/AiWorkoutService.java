package com.joaosantos.workout.service;

import com.joaosantos.workout.dto.ExerciseRecord;
import com.joaosantos.workout.dto.WorkoutPlanRecord;
import com.joaosantos.workout.model.Exercise;
import com.joaosantos.workout.model.UserProfile;
import com.joaosantos.workout.model.WorkoutPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço de orquestração de Inteligência Artificial para treino em casa (Home Workout),
 * utilizando Spring AI e Google Gemini com Structured Output totalmente em Português.
 */
@Service
@Slf4j
public class AiWorkoutService {

    private final ChatClient chatClient;

    public AiWorkoutService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Gera um plano de treino personalizado em casa utilizando o Google Gemini.
     * Focado exclusivamente em peso corporal e objetos domésticos comuns.
     * Todo o conteúdo gerado é estritamente em Português.
     *
     * @param profile dados e objetivos do atleta
     * @return WorkoutPlanRecord estruturado em português
     */
    /**
     * Gera um plano de treino personalizado em casa utilizando o Google Gemini.
     * Focado exclusivamente em peso corporal e objetos domésticos comuns.
     * Todo o conteúdo gerado é estritamente em Português.
     *
     * @param profile dados e objetivos do atleta
     * @return WorkoutPlanRecord estruturado em português
     */
    public WorkoutPlanRecord generateWorkoutForUser(UserProfile profile) {
        return generateWorkoutForUser(profile, null, null, null);
    }

    public WorkoutPlanRecord generateWorkoutForUser(UserProfile profile, String customFitnessLevel, String customFocusArea) {
        return generateWorkoutForUser(profile, customFitnessLevel, customFocusArea, null);
    }

    /**
     * Gera um plano de treino personalizado em casa para uma sessão diária específica,
     * permitindo ao atleta escolher o nível (ex: Intermédio), o foco específico daquele dia,
     * e ajustando a progressão com base no feedback percebido do treino anterior.
     */
    public WorkoutPlanRecord generateWorkoutForUser(
            UserProfile profile,
            String customFitnessLevel,
            String customFocusArea,
            String previousFeedback
    ) {
        String effectiveLevel = (customFitnessLevel != null && !customFitnessLevel.isBlank())
                ? customFitnessLevel : (profile.getFitnessLevel() != null ? profile.getFitnessLevel() : "Intermédio");
        String effectiveFocus = (customFocusArea != null && !customFocusArea.isBlank())
                ? customFocusArea : (profile.getPrimaryGoal() != null ? profile.getPrimaryGoal() : "Corpo Inteiro (Full Body)");

        log.info("A iniciar síntese de treino diário para {} | Nível: {} | Foco: {} | Feedback anterior: {}",
                profile.getName(), effectiveLevel, effectiveFocus, previousFeedback);

        String adaptiveGuidance = "";
        if ("Muito Fácil".equalsIgnoreCase(previousFeedback)) {
            adaptiveGuidance = """
                    NOTA DE PROGRESSÃO ADAPTATIVA:
                    No treino anterior o atleta reportou que o estímulo foi 'Muito Fácil'.
                    Aumenta ligeiramente o desafio, seja com variações mais exigentes de exercícios corporais, mais repetições, menor tempo de descanso ou sobrecarga caseira adicional (mochila com mais peso).
                    """;
        } else if ("Muito Difícil".equalsIgnoreCase(previousFeedback)) {
            adaptiveGuidance = """
                    NOTA DE PROGRESSÃO ADAPTATIVA:
                    No treino anterior o atleta reportou que o estímulo foi 'Muito Difícil'.
                    Ajusta o treino para um ritmo mais sustentável, com exercícios de menor impacto articular, instruções focadas em técnica limpa, pausas conscientes e recuperação controlada.
                    """;
        } else if ("Ideal".equalsIgnoreCase(previousFeedback)) {
            adaptiveGuidance = """
                    NOTA DE PROGRESSÃO ADAPTATIVA:
                    No treino anterior o atleta considerou o treino 'Ideal'. Mantém uma progressão linear equilibrada e estável.
                    """;
        }

        BeanOutputConverter<WorkoutPlanRecord> outputConverter = new BeanOutputConverter<>(WorkoutPlanRecord.class);

        String userPrompt = """
                És um treinador pessoal de elite e especialista em treino funcional e calistenia em casa (Home Workouts).
                Gera um plano de treino de sessão única, altamente eficaz e com 4 a 5 exercícios, adaptado para:
                - Atleta: %s
                - Idade: %d
                - Nível de Condicionamento para esta sessão: %s
                - Área de Foco / Tipo de Treino desta sessão: %s

                %s

                REGRAS OBRIGATÓRIAS:
                1. O treino é EXCLUSIVAMENTE para fazer EM CASA.
                2. A sessão DEVE focar-se rigorosamente no tipo de treino: "%s". Todos os 4 a 5 exercícios devem ser direcionados especificamente a trabalhar esta área de foco (exemplo: se for Membros Superiores/Peito/Braços/Costas, usar flexões, fundos em cadeira, remadas com toalha; se for Membros Inferiores/Pernas/Glúteos, agachamentos búlgaros, passadas com garrafas, gémeos; se for Core/Abdominais, pranchas, abdominais em tapete, dead bug; se for Cardio HIIT, polichinelos, escaladores dinâmicos, burpees suaves; se for Mobilidade, rotações articulares e alongamentos).
                3. UTILIZA APENAS o peso corporal (calistenia) e objetos comuns que QUALQUER PESSOA tem em casa (ex: cadeira firme, sofá, garrafas de água, toalha, mochila com livros, parede, chão/tapete).
                4. É ESTRITAMENTE PROIBIDO utilizar ou sugerir qualquer equipamento de ginásio comercial (NÃO usar halteres, barras, máquinas, polias ou pesos de ginásio).
                5. O idioma da resposta DEVE SER 100%% EM PORTUGUÊS (título estimulante, área de foco "%s", explicação da estratégia, nomes claros dos exercícios, repetições/tempo, equipamento doméstico e instruções detalhadas de técnica e postura adaptadas ao nível %s).
                6. Dá dicas claras de postura (instruções) para evitar lesões e maximizar a ativação muscular.

                %s
                """.formatted(
                profile.getName(),
                profile.getAge(),
                effectiveLevel,
                effectiveFocus,
                adaptiveGuidance,
                effectiveFocus,
                effectiveFocus,
                effectiveLevel,
                outputConverter.getFormat()
        );

        try {
            log.debug("A enviar prompt estruturado para o Google Gemini via Spring AI ChatClient");

            String response = this.chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();

            log.debug("Resposta recebida do LLM. A desserializar para WorkoutPlanRecord");

            WorkoutPlanRecord workoutPlanRecord = outputConverter.convert(response);

            log.info("Treino em casa sintetizado com sucesso: '{}' com {} exercícios para {}",
                    workoutPlanRecord.title(),
                    workoutPlanRecord.exercises() != null ? workoutPlanRecord.exercises().size() : 0,
                    profile.getName());

            return workoutPlanRecord;

        } catch (Exception ex) {
            log.warn("Gemini API em sobrecarga temporária ({}). A aplicar motor de síntese resiliente em casa.",
                    ex.getMessage());
            return generateResilientHomeFallbackPlan(profile, effectiveLevel, effectiveFocus);
        }
    }

    /**
     * Substitui um exercício específico do plano por uma alternativa equivalente em casa,
     * utilizando a IA do Google Gemini.
     */
    public ExerciseRecord replaceExercise(Exercise currentExercise, WorkoutPlan plan, UserProfile profile) {
        log.info("A solicitar substituição por IA para o exercício '{}' do plano '{}' ({})",
                currentExercise.getName(), plan.getTitle(), plan.getFocusArea());

        BeanOutputConverter<ExerciseRecord> outputConverter = new BeanOutputConverter<>(ExerciseRecord.class);

        String userPrompt = """
                És um treinador pessoal de elite e especialista em treino funcional em casa (Home Workouts).
                O atleta precisa de substituir um exercício do seu treino de hoje por uma alternativa:
                - Exercício a substituir: %s
                - Séries atuais: %d
                - Repetições atuais: %s
                - Equipamento doméstico atual: %s
                - Foco / Grupo Muscular da sessão: %s
                - Nível do Atleta: %s

                REGRAS OBRIGATÓRIAS:
                1. Devolve EXATAMENTE UM (1) novo exercício alternativo eficaz para o mesmo grupo muscular.
                2. O exercício deve ser COMPLETAMENTE DIFERENTE de "%s".
                3. É EXCLUSIVAMENTE para fazer EM CASA (apenas peso corporal ou objetos domésticos: cadeira, sofá, toalha, garrafas de água, mochila com livros, tapete, chão, parede).
                4. É ESTRITAMENTE PROIBIDO sugerir pesos de ginásio, halteres, barras ou máquinas comerciais.
                5. A resposta DEVE SER 100%% EM PORTUGUÊS (nome claro, número de séries coerente, repetições/tempo, equipamento caseiro e instruções de técnica).

                %s
                """.formatted(
                currentExercise.getName(),
                currentExercise.getSets(),
                currentExercise.getReps(),
                currentExercise.getEquipmentNeeded(),
                plan.getFocusArea(),
                profile.getFitnessLevel(),
                currentExercise.getName(),
                outputConverter.getFormat()
        );

        try {
            String response = this.chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();

            ExerciseRecord replacement = outputConverter.convert(response);
            log.info("Exercício substituído por IA com sucesso: '{}'", replacement.name());
            return replacement;
        } catch (Exception ex) {
            log.warn("Falha na chamada Gemini para substituir exercício ({}). A aplicar alternativa em casa.", ex.getMessage());
            return generateResilientExerciseFallback(currentExercise, plan.getFocusArea());
        }
    }

    /**
     * Fornece um exercício alternativo resiliente caso a IA esteja temporariamente indisponível.
     */
    private ExerciseRecord generateResilientExerciseFallback(Exercise current, String focusArea) {
        String name = current.getName().toLowerCase();
        String focus = focusArea != null ? focusArea.toLowerCase() : "";

        if (name.contains("flexão") || name.contains("peito") || name.contains("push")) {
            return new ExerciseRecord(
                    "Fundos de Tríceps e Peitoral no Sofá / Cadeira Firme",
                    4,
                    "10 a 12 reps",
                    "Sofá ou Cadeira estável",
                    "Apoia as palmas na borda do sofá. Desce os cotovelos a 90 graus mantendo as costas alinhadas e empurra com força para subir."
            );
        } else if (name.contains("agacha") || name.contains("squat") || name.contains("bulg")) {
            return new ExerciseRecord(
                    "Passadas (Lunges) com Carga de Mochila ao Peito",
                    4,
                    "12 reps por perna",
                    "Mochila com livros ou Garrafa de água",
                    "Dá um passo largo à frente, desce o joelho de trás quase até ao chão mantendo o tronco ereto e volta à posição inicial com o calcanhar frontal."
            );
        } else if (name.contains("prancha") || name.contains("plank") || name.contains("abdom")) {
            return new ExerciseRecord(
                    "Dead Bug (Inseto Morto) com Contração Abdominal",
                    3,
                    "12 reps alternadas",
                    "Tapete no chão",
                    "Deitado de costas, estica o braço direito e a perna esquerda ao mesmo tempo, mantendo a lombar colada ao tapete sem arquear as costas."
            );
        } else if (name.contains("remada") || name.contains("toalha") || name.contains("costa")) {
            return new ExerciseRecord(
                    "Superman no Chão com Pausa de 2 Segundos",
                    4,
                    "12 a 15 reps",
                    "Tapete ou toalha no chão",
                    "Deitado de barriga para baixo, eleva simultaneamente o peito e as coxas do chão, contraindo as omoplatas, lombar e glúteos no ponto mais alto."
            );
        } else {
            return new ExerciseRecord(
                    "Agachamento com Salto Suave (Jump Squat)",
                    4,
                    "10 a 12 reps",
                    "Apenas Peso Corporal",
                    "Agacha até à paralela e salta suavemente na subida, amortecendo com a ponta dos pés para proteger as articulações."
            );
        }
    }

    /**
     * Motor resiliente de síntese de treinos em casa em Português com objetos domésticos,
     * garantindo 100% de disponibilidade caso a API externa sofra picos de tráfego.
     */
    private WorkoutPlanRecord generateResilientHomeFallbackPlan(UserProfile profile, String level, String focus) {
        String normalizedFocus = focus != null ? focus.toLowerCase() : "";
        String title;
        String focusArea;
        String explanation;
        List<ExerciseRecord> exercises;

        if (normalizedFocus.contains("superior") || normalizedFocus.contains("peito") || normalizedFocus.contains("braço") || normalizedFocus.contains("costa")) {
            title = "Treino Superior de Força & Peitoral em Casa";
            focusArea = "Membros Superiores (Peito, Costas, Ombros e Braços)";
            explanation = "Rotina diária para %s (%d anos, nível %s) com foco nos músculos da parte superior do corpo, utilizando sobrecargas com garrafas e alavancas corporais em móveis firmes.".formatted(
                    profile.getName(), profile.getAge(), level);
            exercises = List.of(
                    new ExerciseRecord(
                            "Flexões de Braço no Chão (ou com mãos no sofá)",
                            4,
                            "10 a 15 reps",
                            "Tapete ou Borda do Sofá",
                            "Apoia as mãos à largura dos ombros. Mantém o corpo em linha reta e abdómen ativado. Desce o peito até quase tocar no chão e sobe com força contraindo o peitoral."
                    ),
                    new ExerciseRecord(
                            "Fundos de Tríceps na Cadeira Firme",
                            4,
                            "10 a 12 reps",
                            "Cadeira estável ou Sofá",
                            "Apoia as palmas na borda do assento. Desce o corpo dobrando os cotovelos a 90 graus rente à cadeira e sobe estendendo os braços com foco nos tríceps."
                    ),
                    new ExerciseRecord(
                            "Remada com Toalha na Porta Fechada",
                            4,
                            "10 a 12 reps",
                            "Toalha grossa presa no puxador da porta fechada",
                            "Passa a toalha no trinco de uma porta bem fechada. Inclina o corpo para trás com os calcanhares fixos e puxa o peito em direção à porta apertando as omoplatas."
                    ),
                    new ExerciseRecord(
                            "Elevações Laterais de Ombros com Garrafas de Água",
                            4,
                            "12 a 15 reps",
                            "2 Garrafas de 1.5L ou 2L",
                            "Em pé com boa postura, levanta os braços de lado até à linha dos ombros com os cotovelos ligeiramente fletidos. Controla a descida sem embalo."
                    ),
                    new ExerciseRecord(
                            "Bíceps Concentrado com Mochila com Livros",
                            3,
                            "12 a 15 reps",
                            "Mochila com livros ou Garrafão de 5L",
                            "Segura a alça da mochila com os cotovelos colados ao tronco. Flete os braços até ao peito e desce devagar em 3 segundos sentindo o bíceps trabalhar."
                    )
            );
        } else if (normalizedFocus.contains("inferior") || normalizedFocus.contains("perna") || normalizedFocus.contains("glúteo")) {
            title = "Treino Potente de Pernas & Glúteos em Casa";
            focusArea = "Membros Inferiores (Pernas, Glúteos e Gémeos)";
            explanation = "Sessão focada na cadeia inferior para %s (%d anos, nível %s) garantindo hipertrofia, força e tonificação através de exercícios unilaterais e sobrecarga com peso corporal.".formatted(
                    profile.getName(), profile.getAge(), level);
            exercises = List.of(
                    new ExerciseRecord(
                            "Agachamento Búlgaro no Sofá / Cadeira",
                            4,
                            "10 a 12 reps por perna",
                            "Sofá ou Cadeira firme",
                            "Apoia o peito de um pé atrás no sofá. Com a perna da frente, desce até à coxa ficar paralela ao chão, mantendo o tronco firme. Foco intenso em quadríceps e glúteos."
                    ),
                    new ExerciseRecord(
                            "Passadas (Lunges) com Garrafas de Água",
                            4,
                            "12 reps por perna",
                            "Duas garrafas de 1.5L ou 2L",
                            "Dá um passo largo em frente e flete ambos os joelhos a 90 graus. Mantém o peito aberto e empurra o calcanhar da frente para voltar à posição inicial."
                    ),
                    new ExerciseRecord(
                            "Elevação Pélvica / Ponte de Glúteos Unilateral",
                            4,
                            "12 a 15 reps por perna",
                            "Tapete ou Toalha no chão",
                            "Deitado de costas, eleva uma perna no ar e empurra o chão com o outro calcanhar, elevando a anca ao máximo e contraindo o glúteo por 2 segundos no topo."
                    ),
                    new ExerciseRecord(
                            "Cadeira Estática na Parede (Wall Sit)",
                            3,
                            "30 a 45 segundos",
                            "Parede livre",
                            "Encosta as costas na parede e desliza até os joelhos ficarem em ângulo de 90 graus, como se estivesses sentado numa cadeira invisível. Mantém a respiração firme."
                    ),
                    new ExerciseRecord(
                            "Elevação de Gémeos na Ponta dos Pés num Degrau",
                            4,
                            "15 a 20 reps",
                            "Degrau da porta ou livro grosso no chão",
                            "Apoia a ponta dos pés na borda de um degrau com a mão na parede para equilíbrio. Desce os calcanhares para alongar e sobe na ponta máxima dos pés apertando os gémeos."
                    )
            );
        } else if (normalizedFocus.contains("core") || normalizedFocus.contains("abdominal") || normalizedFocus.contains("abdómen")) {
            title = "Treino Intensivo de Core & Estabilidade Abdominal";
            focusArea = "Core & Abdominais (Tronco e Postura)";
            explanation = "Rotina desenhada para %s (%d anos, nível %s) para esculpir o abdómen, fortalecer a cinta lombar e melhorar a estabilidade e postura diária.".formatted(
                    profile.getName(), profile.getAge(), level);
            exercises = List.of(
                    new ExerciseRecord(
                            "Prancha Abdominal Tradicional com Contração Máxima",
                            4,
                            "30 a 50 segundos",
                            "Tapete ou Toalha no chão",
                            "Apoia os antebraços e as pontas dos pés no chão. Aperta o umbigo contra a coluna e contrai glúteos e pernas. Não deixes a bacia descer nem subir demais."
                    ),
                    new ExerciseRecord(
                            "Abdominal Bicicleta (Bicycle Crunches)",
                            4,
                            "20 reps alternadas",
                            "Tapete no chão",
                            "Deitado de costas com as mãos na nuca, leva o cotovelo direito ao joelho esquerdo esticando a outra perna, alternando de forma contínua e controlada."
                    ),
                    new ExerciseRecord(
                            "Dead Bug (Inseto Morto) para Estabilidade Lombar",
                            3,
                            "12 reps de cada lado",
                            "Tapete no chão",
                            "Deitado de costas com braços para cima e joelhos a 90 graus. Baixa o braço direito para trás e a perna esquerda para a frente sem descolar a lombar do chão."
                    ),
                    new ExerciseRecord(
                            "Prancha Lateral com Elevação da Anca",
                            3,
                            "10 a 12 reps de cada lado",
                            "Tapete no chão",
                            "Apoiado num antebraço de lado, sobe e desce a anca mantendo o alinhamento corporal perfeito para ativar os oblíquos."
                    ),
                    new ExerciseRecord(
                            "Escaladores Lentos com Foco no Core",
                            3,
                            "30 a 40 segundos",
                            "Chão firme",
                            "Em posição de prancha alta, puxa um joelho de cada vez em direção ao cotovelo oposto devagar, esmagando o abdómen a cada repetição."
                    )
            );
        } else if (normalizedFocus.contains("cardio") || normalizedFocus.contains("hiit") || normalizedFocus.contains("gordura") || normalizedFocus.contains("calorias")) {
            title = "Circuito Metabólico HIIT Queima-Gordura em Casa";
            focusArea = "Cardio HIIT (Resistência e Queima de Calorias)";
            explanation = "Sessão de alta densidade e ritmo acelerado para %s (%d anos, nível %s) acelerar o metabolismo e o consumo calórico sem necessidade de máquinas.".formatted(
                    profile.getName(), profile.getAge(), level);
            exercises = List.of(
                    new ExerciseRecord(
                            "Polichinelos Dinâmicos (Jumping Jacks)",
                            4,
                            "40 segundos ativos (20s descanso)",
                            "Apenas Peso Corporal",
                            "Salta abrindo braços e pernas de forma ritmada, amortecendo suavemente nas pontas dos pés."
                    ),
                    new ExerciseRecord(
                            "Mountain Climbers (Escaladores Rápidos)",
                            4,
                            "30 a 40 segundos",
                            "Tapete ou chão",
                            "Em prancha alta, corre no chão puxando os joelhos alternadamente em direção ao peito com intensidade contínua."
                    ),
                    new ExerciseRecord(
                            "Agachamento com Extensão Rápida de Calcanhares",
                            4,
                            "15 a 20 reps",
                            "Apenas Peso Corporal",
                            "Agacha com profundidade e na subida sobe imediatamente na ponta dos pés de forma explosiva."
                    ),
                    new ExerciseRecord(
                            "Shadow Boxing (Golpes de Boxe Rítmicos no Ar)",
                            3,
                            "45 segundos contínuos",
                            "Espaço livre",
                            "Na postura de guarda, desfere socos alternados rápidos à frente com rotação do tronco e abdómen bem tenso."
                    ),
                    new ExerciseRecord(
                            "Step-Up Alternado no Sofá ou Degrau Firme",
                            3,
                            "15 reps por perna",
                            "Sofá firme ou Degrau estável",
                            "Sobe e desce com vigor alternando a perna condutora, mantendo o ritmo cardíaco elevado."
                    )
            );
        } else if (normalizedFocus.contains("mobilidade") || normalizedFocus.contains("alongamento") || normalizedFocus.contains("recuperação")) {
            title = "Sessão de Mobilidade & Recuperação Articular em Casa";
            focusArea = "Mobilidade, Flexibilidade & Alongamento";
            explanation = "Rotina regenerativa para %s (%d anos, nível %s) para soltar tensões musculares, ampliar a flexibilidade e prevenir dores corporais.".formatted(
                    profile.getName(), profile.getAge(), level);
            exercises = List.of(
                    new ExerciseRecord(
                            "Postura Gato-Vaca (Cat-Cow) para a Coluna",
                            3,
                            "10 ciclos respiratórios lentos",
                            "Tapete no chão",
                            "De quatro apoios, arqueia a coluna para cima expirando e depois deixa a coluna arquear suavemente para baixo inspirando e olhando em frente."
                    ),
                    new ExerciseRecord(
                            "Alongamento de Peitoral no Batente da Porta",
                            3,
                            "30 segundos por braço",
                            "Batente de uma porta aberta",
                            "Apoia o antebraço no batente da porta e dá um pequeno passo à frente, rodando ligeiramente o tronco para alongar o peitoral e o ombro."
                    ),
                    new ExerciseRecord(
                            "Alongamento de Flexores da Anca (Lunge Baixo no Chão)",
                            3,
                            "30 segundos por perna",
                            "Tapete com almofada sob o joelho",
                            "Dá um passo largo em frente com o joelho de trás apoiado suavemente numa almofada. Empurra a anca para a frente e para baixo sentindo o quadril alongar."
                    ),
                    new ExerciseRecord(
                            "Rotações Torácicas em Quatro Apoios",
                            3,
                            "8 a 10 rotações de cada lado",
                            "Tapete no chão",
                            "Mão na nuca, roda o cotovelo para o chão e depois abre o peito apontando o cotovelo para o teto, acompanhando com o olhar."
                    ),
                    new ExerciseRecord(
                            "Postura da Criança (Child's Pose) com Braços Estendidos",
                            3,
                            "45 a 60 segundos de respiração profunda",
                            "Tapete no chão",
                            "Senta-te sobre os calcanhares e estende os braços para a frente no chão, pousando a testa no tapete e relaxando as costas e os ombros."
                    )
            );
        } else {
            // Corpo Inteiro (Full Body Calistenia & Objetos Caseiros)
            title = "Treino Funcional de Corpo Inteiro (Full Body) em Casa";
            focusArea = "Corpo Inteiro (Calistenia & Objetos Caseiros)";
            explanation = "Treino equilibrado para %s (%d anos, nível %s) que ativa todos os grandes grupos musculares com peso corporal e cargas caseiras improvisadas.".formatted(
                    profile.getName(), profile.getAge(), level);
            exercises = List.of(
                    new ExerciseRecord(
                            "Flexões de Braço Tradicionais",
                            4,
                            "10 a 15 reps",
                            "Tapete ou chão",
                            "Corpo perfeitamente alinhado. Desce em 2 segundos até o peito quase tocar no chão e sobe com potência contraindo o peitoral."
                    ),
                    new ExerciseRecord(
                            "Agachamento com Mochila Carregada de Livros",
                            4,
                            "12 a 15 reps",
                            "Mochila com peso ou garrafão",
                            "Coloca a mochila às costas ou segura-a junto ao peito. Agacha para trás mantendo os joelhos alinhados com as pontas dos pés."
                    ),
                    new ExerciseRecord(
                            "Remada com Toalha na Porta Fechada",
                            4,
                            "10 a 12 reps",
                            "Toalha grossa no puxador da porta",
                            "Inclina o corpo para trás e puxa o tronco em direção à porta com força nas costas e omoplatas."
                    ),
                    new ExerciseRecord(
                            "Fundos de Tríceps na Cadeira Firme",
                            3,
                            "10 a 12 reps",
                            "Cadeira ou Sofá",
                            "Costas rentes à cadeira, flete os cotovelos a 90 graus e empurra com a força dos braços."
                    ),
                    new ExerciseRecord(
                            "Prancha Abdominal com Toque nos Ombros",
                            3,
                            "30 a 45 segundos",
                            "Tapete no chão",
                            "Em prancha alta, toca alternadamente nos ombros com as mãos sem deixar a anca oscilar."
                    )
            );
        }

        return new WorkoutPlanRecord(title, focusArea, explanation, exercises);
    }
}
