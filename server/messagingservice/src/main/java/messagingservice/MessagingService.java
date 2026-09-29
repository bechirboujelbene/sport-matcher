package messagingservice;

import messagingservice.entity.MessageEntity;
import messagingservice.repository.MessageRepository;
import messagingservice.repository.ContactRepository;
import messagingservice.client.UserServiceClient;
import messagingservice.entity.ContactEntity;
import messagingservice.dto.ContactDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.List;
import messagingservice.dto.MessageDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

@Service
public class MessagingService {

    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ContactRepository contactRepository;
    private final UserServiceClient userClient;

    @Autowired
    public MessagingService(MessageRepository messageRepository,
                           SimpMessagingTemplate messagingTemplate,
                           ContactRepository contactRepository,
                           UserServiceClient userClient) {
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.contactRepository = contactRepository;
        this.userClient = userClient;
    }

    /**
     * Persist a new message and return the saved entity.
     */
    public MessageDto sendMessage(String fromUserId, String toUserId, String content) {
        MessageEntity entity = new MessageEntity(fromUserId, toUserId, content, LocalDateTime.now());
        MessageEntity saved = messageRepository.save(entity);
        MessageDto dto = toDto(saved);
        messagingTemplate.convertAndSendToUser(fromUserId, "/queue/messages", dto);
        if (!fromUserId.equals(toUserId)) {
            messagingTemplate.convertAndSendToUser(toUserId, "/queue/messages", dto);
        }
        return dto;
    }

    /**
     * Retrieve the full conversation between the two users sorted by timestamp.
     */
    public Page<MessageDto> getConversation(String userA, String userB, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findConversation(userA, userB, pageable).map(this::toDto);
    }

    /**
     * Add a contact relationship (bidirectional).
     */
    @Transactional
    public ContactDto addContact(String userId, String contactId) {
        if (!contactRepository.existsByUserIdAndContactId(userId, contactId)) {
            ContactEntity c1 = new ContactEntity(userId, contactId, LocalDateTime.now());
            ContactEntity c2 = new ContactEntity(contactId, userId, LocalDateTime.now());
            contactRepository.save(c1);
            contactRepository.save(c2);
        }
        return new ContactDto(userId, contactId, LocalDateTime.now());
    }

    /**
     * Get full UserDTO list of a user's contacts.
     */
    public List<model.UserDTO> getContacts(String userId) {
        List<String> ids = contactRepository.findContactIdsByUser(userId);
        return userClient.getUsers(ids);
    }

    private MessageDto toDto(MessageEntity e) {
        return new MessageDto(e.getId(), e.getFromUserId(), e.getToUserId(), e.getContent(), e.getTimestamp());
    }
}
