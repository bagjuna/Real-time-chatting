package com.chat.persistence.repository

import com.chat.domain.model.ChatRoomMember
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ChatRoomMemberRepository : CrudRepository<ChatRoomMember, String> {
    fun findByChatRoomIdAndIsActiveTrue(chatRoomId: Long): List<ChatRoomMember>

    fun findBychatRoomIdAndUserIdAndIsActiveTrue(chatRoomId: Long, userId: Long): Optional<ChatRoomMember>

    @Query("select count(crm) from ChatRoomMember crm where crm.chatRoom.id = :chatRoomId and crm.isActive = true")
    fun countActiveMembersInRoom(chatRoomId: Long): Long

    @Modifying
    @Query("""
        UPDATE ChatRoomMember crm 
        SET crm.isActive = false, crm.leftAt = CURRENT_TIMESTAMP
        WHERE crm.chatRoom.id = :chatRoomId AND crm.user.id = :userId
    """)
    fun leaveChatRoom(chatRoomId: Long, userId: Long)

    /*
     1. A라는 Entity를 가져 왓다.
     2. A에 매칭이 되는 Raw를 수정을 하였습니다. -> DB를 수정
     3. A에 대한 로그를 찍으면 X가 노출
  */

    fun existsByChatRoomIdAndUserIdAndIsActiveTrue(chatRoomId: Long, userId: Long): Boolean

}
