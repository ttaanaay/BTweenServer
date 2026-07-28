package com.btween.server.exception

class ValidationException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
class ConflictException(message: String) : Exception(message)
class UnauthorizedException(message: String) : Exception(message)
class ForbiddenException(message: String) : Exception(message)
