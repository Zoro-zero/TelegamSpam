
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

class MyTelegramBot : TelegramLongPollingBot() {
    private val logger = LoggerFactory.getLogger(MyTelegramBot::class.java)
    private val requiredChannelId = "@TestBent"

    override fun getBotUsername(): String = "SpamZeroBot"
    override fun getBotToken(): String = "6938133058:AAGLm0SBSR_3emup3kEaVblZ67wyTmXZoPw"
    override fun onUpdateReceived(update: Update) {
        logger.info("Received update: $update")

        val message = update.message
        if (message != null && message.hasText()) {
            val userId = message.from.id
            val chatId = message.chatId

            logger.info("Received message from user $userId in chat $chatId")

            // Пытаемся проверить подписку
            try {
                if (!checkSubscription(userId)) {
                    logger.info("User $userId is not subscribed to the channel.")
                    deleteMessage(chatId, message.messageId)
                    sendMessage(chatId, "Вы должны подписаться на канал $requiredChannelId, чтобы писать здесь.")
                } else {
                    logger.info("User $userId is subscribed to the channel.")
                }
            } catch (e: Exception) {
                logger.error("Error processing update: $update", e)
            }
        }
    }

    private fun checkSubscription(userId: Long): Boolean {
        return try {
            val chatMember = execute(GetChatMember(requiredChannelId, userId))
            when (chatMember.status) {
                "member", "administrator", "creator" -> true
                else -> false
            }
        } catch (e: TelegramApiException) {
            logger.error("Failed to check subscription for user $userId in channel $requiredChannelId", e)
            false
        }
    }

    private fun deleteMessage(chatId: Long, messageId: Int) {
        val deleteMessage = DeleteMessage(chatId.toString(), messageId)
        try {
            execute(deleteMessage)
            logger.info("Deleted message $messageId in chat $chatId")
        } catch (e: TelegramApiException) {
            logger.error("Failed to delete message $messageId in chat $chatId", e)
        }
    }

    private fun sendMessage(chatId: Long, text: String) {
        val sendMessage = SendMessage(chatId.toString(), text)
        try {
            val sentMessage = execute(sendMessage)
            logger.info("Sent message to chat $chatId: $text")

            Thread {
                try {
                    Thread.sleep(5000)
                    deleteMessage(chatId, sentMessage.messageId)
                } catch (e: InterruptedException) {
                    logger.error("Error during sleep", e)
                }
            }.start()
        } catch (e: TelegramApiException) {
            logger.error("Failed to send message to chat $chatId", e)
        }
    }
}

fun main() {
    val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
    try {
        botsApi.registerBot(MyTelegramBot())
        println("Bot started successfully")
    } catch (e: TelegramApiException) {
        e.printStackTrace()
    }
}