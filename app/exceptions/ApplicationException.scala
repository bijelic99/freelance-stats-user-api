package exceptions

sealed trait ApplicationException extends Exception

object ApplicationException {
  case object UserAlreadyExists extends ApplicationException
  case object WrongUsernameOrPassword extends ApplicationException
  case object EitherNewOrOldPasswordIncorrect extends ApplicationException
  case object UserNotFound extends ApplicationException
  case object CantDeleteViaUpdate extends ApplicationException
}
