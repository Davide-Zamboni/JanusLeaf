package com.janusleaf.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class UserAlreadyExistsException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class InvalidCredentialsException(message: String = "Invalid email or password") : RuntimeException(message)

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class InvalidTokenException(message: String = "Invalid or expired token") : RuntimeException(message)

@ResponseStatus(HttpStatus.NOT_FOUND)
class UserNotFoundException(message: String = "User not found") : RuntimeException(message)

@ResponseStatus(HttpStatus.NOT_FOUND)
class NoteNotFoundException(message: String = "Note not found") : RuntimeException(message)

@ResponseStatus(HttpStatus.FORBIDDEN)
class NoteAccessDeniedException(message: String = "You don't have access to this note") : RuntimeException(message)

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class AIServiceException(message: String = "AI service is temporarily unavailable") : RuntimeException(message)
