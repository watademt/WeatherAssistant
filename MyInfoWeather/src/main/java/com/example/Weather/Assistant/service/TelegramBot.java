package com.example.Weather.Assistant.service;

import com.example.Weather.Assistant.config.BotConfig;
import com.example.Weather.Assistant.model.User;
import com.example.Weather.Assistant.model.Weather;
import com.example.Weather.Assistant.repository.UserRepository;
import com.example.Weather.Assistant.repository.UserState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    final BotConfig config;
    private final Weather weather = new Weather();


    static final  String CHANGE_CITY = "Напишите название населенного пункта, в котором вас интересует погода";
    static final String HELP_TEXT = "Данный бот показывает погоду и помогает советами...";

    public TelegramBot(BotConfig config){
        this.config = config;
        List<BotCommand> listofCommand = new ArrayList<>();
        listofCommand.add(new BotCommand("/start","старт бота"));
        listofCommand.add(new BotCommand("/help","информация как использовать бота"));
        listofCommand.add(new BotCommand("/city","изменить населенный пункт"));
        listofCommand.add(new BotCommand("/subscription","подписка на рассылку погоды"));
        listofCommand.add(new BotCommand("/unsubscribe","отключение подписки на рассылку погоды"));
        listofCommand.add(new BotCommand("/changetime","отключение подписки на рассылку погоды"));
        try{
            this.execute(new SetMyCommands(listofCommand, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e){
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken(){
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatID = update.getMessage().getChatId();

            switch (messageText){
                case "/start":
                    registerUser(update.getMessage());
                    changeUserState(chatID, update);
                    startCommand(chatID);
                    break;
                case "/help":
                    sendMessage(chatID, HELP_TEXT);
                    break;
                case "/city":
                    changeUserState(chatID, update);
                    break;
                case "/subscription":
                    changeUserState(chatID, update);
                    toSubscription(chatID);
                    break;
                case "/unsubscribe":
                    unsubscribe(chatID);
                    break;
                case "/changetime":
                    changeUserState(chatID, update);
                        changeTime(chatID);
                    break;
                default:
                    User user = userRepository.findById(chatID).get();
                    if (user.getUserState() == UserState.CityChanges) {
                        sendCity(user, update);
                        user.setUserState(UserState.Default);
                    }
                    else if (user.getUserState() == UserState.Subscription){
                        registerTimeSubscription(user, update);
                        user.setUserState(UserState.Default);
                    } else if (user.getUserState() == UserState.TimeZone) {
                        timeZone(user, update);
                        user.setUserState(UserState.Default);
                    } else {
                        sendMessage(chatID, "Мы не можем распознать это сообщение, используете комнады");
                    }
            }
        }

    }

    /**
     * изменение времени рассылки
     * @param chatID чат id пользователя
     */
    private void changeTime(Long chatID){
        User user = userRepository.findById(chatID).get();
        if (user.getSubscription()) {
            sendMessage(chatID, "Напишите время, в которое хотите получаеть прогноз погоды (например, 14:00)");
        }
        else {
            sendMessage(chatID, "У вас не подключена подписка, воспользуйтесь командой /subscription");
        }
    }
    /**
     * сохраняет часовой пояс пользователя
     * @param user пользователь
     * @param update используется для получения сообщения
     */
    private void timeZone(User user, Update update){
        user.setTimeZone(Integer.parseInt(update.getMessage().getText()));
        userRepository.save(user);
        sendMessage(user.getChatID(),"Время сохранено, воспользуйтесь командой /city для продолжения");
    }
    /**
     * отключение подписки
     * @param chatID чат id пользователя
     */
    private void unsubscribe(Long chatID){
        User user = userRepository.findById(chatID).get();
        user.setSubscription(false);
        userRepository.save(user);

        sendMessage(chatID, "Рассылка отключена!");
    }
    /**
     * сохраняет нужное время для рассылки
     * @param user пользователь
     * @param update используется для получения сообщения
     */

    private void  registerTimeSubscription(User user, Update update){
        String time = update.getMessage().getText();
        user.setTimeSubscription(time);
        userRepository.save(user);

        sendMessage(user.getChatID(), "Время сохранено!");
    }
    /**
     * сохраняет в бд начличие подписки
     * @param chatID id нужного чата
     */
    private void toSubscription(Long chatID){
        User user = userRepository.findById(chatID).get();
        user.setSubscription(true);
        userRepository.save(user);
        sendMessage(chatID, "Напишите время, в которое хотите получаеть прогноз погоды (например, 14:00)");
    }

    /**
     * меняет состояние пользователя
     * @param chatID id нужного чата
     * @param update используется для получения сообщения
     */
    private void changeUserState(Long chatID, Update update){
        if (userRepository.findById(chatID).isPresent()){
            if(update.getMessage().getText().equals("/city")){
                User user = userRepository.findById(chatID).get();
                user.setUserState(UserState.CityChanges);
                userRepository.save(user);
                sendMessage(chatID, CHANGE_CITY);
            }
            else if(update.getMessage().getText().equals("/subscription") | update.getMessage().getText().equals("/changetime")){
                User user = userRepository.findById(chatID).get();
                user.setUserState(UserState.Subscription);
                userRepository.save(user);
            } else if (update.getMessage().getText().equals("/start")) {
                User user = userRepository.findById(chatID).get();
                user.setUserState(UserState.TimeZone);
                userRepository.save(user);
            }
        }
        else {
            sendMessage(chatID, "Вы не зарегистрированы! Используете команду /start");
        }
    }
    /**
     * меняет город в базе данных и выдает погоду в выбраном городе
     * @param user пользователь
     * @param update используется для получения сообщения
     */
    private void sendCity(User user, Update update){
        String city = update.getMessage().getText();

        user.setCity(city);

        userRepository.save(user);
        log.info("city saved");

        sendMessage(user.getChatID(), "Вы выбрали населенный пункт - " + city);
        sendMessage(user.getChatID(), weather.getWeather(city));
        sendMessage(user.getChatID(),"Для рассылки погоды каждый день воспользуйтесь командой /subscription");
    }

    /**
     * регистрирует пользователя и вносит его в базу данных
     * @param message
     */
    private void registerUser(Message message) {

        if (userRepository.findById(message.getChatId()).isEmpty()){

            var chatID = message.getChatId();
            var chat = message.getChat();

            User user = new User();
            user.setChatID(chatID);
            user.setNameUser(chat.getFirstName());

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    /**
     * выдвет сообщения при команде /start
     * @param chatID
     */
    private void startCommand(long chatID){
        User user = userRepository.findById(chatID).get();
        String textToSend = "Привет, " + user.getNameUser() + ", укажите ваш часовой пояс относительно Москва (например, 2)";

        log.info("User: " + user.getNameUser());

        sendMessage(chatID, textToSend);
    }

    /**
     * генирирует сообщения
     * @param chatID
     * @param textToSend
     */
    private void sendMessage(Long chatID, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(chatID);
        message.setText(textToSend);

        try {
            execute(message);
        }
        catch (TelegramApiException e){
            log.error("Error: " + e.getMessage());
        }
    }
    /**
     * получание погоды для рассылки
     * @param user пользователь
     */
    private void getWeather(User user){
        if(user.getSubscription()){
            sendMessage(user.getChatID(), weather.getWeather(user.getCity()));
        }
    }
    /**
     * проверка времени рассылки, если оно совпадает с нынешнем временем происходит россылка
     */
    @Scheduled(cron = "0 * * * * *")
    private void checkTime(){
        var users = userRepository.findAll();
        for (User user: users){
            if(user.getSubscription()) {
                DateTimeFormatter formater = DateTimeFormatter.ofPattern("HH:mm");

                String time = user.getTimeSubscription();
                LocalTime time1 = LocalTime.now().plusHours(user.getTimeZone() - 2);

                if (time.equals(time1.format(formater))) {
                    getWeather(user);
                }
            }
        }
    }
}
