
import akka.actor._
import akka.actor.Props
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.actor.ActorSystem


object Main extends App {
  val currentDir = System.getProperty("user.dir")
  val directory = new java.io.File(s"${currentDir}/src/main/resources/blogs")

  val regex = "is".r

  Actor.controllerActor ! Traverse(directory)
}

class CounterActor extends Actor {
  var matchingWords = 0

  def receive = {
    case Match => {
      matchingWords = matchingWords + 1
    }
    case CurrentTotal => sender ! matchingWords
  }
}

class Controller extends Actor {

  def receive = {
    case Traverse(directory) =>  {
      for {
        dir <- directory.listFiles()
      }
      {
        if(dir.getPath().endsWith(".md")) {
          println(dir.isDirectory)
          Actor.readFileActor ! File(dir)
        }
      }

    }
  }

}


class SubDirectories extends Actor {

  var children:Set[ActorRef] = Set.empty[ActorRef]

  def receive = {
    case Check(directory) => {
      for {
        dir <- directory.listFiles()
      }
      {
        if(dir.getPath().endsWith(".md")) {
          context.actorOf(Props(new ReadFile(dir)))
        }
      }
    }
  }
}

class ReadFile(file: java.io.File) extends Actor {
  def receive = {
    case File(file) => {
      println("read file msg")
      for {
        line <- scala.io.Source.fromFile(file).getLines
        word <- line.split(" ")
      }{
        Main.regex.findFirstIn(word).foreach{ _ =>
            Actor.countActor ! Match
        }
      }
      sender ! Finished
    }
  }
}


case object Match
case object CurrentTotal
case class Check(val directory: java.io.File)
case class Traverse(val directory: java.io.File)
case object Finished
case class File(val file: java.io.File)

object Actor {
  // ActorSystem is a heavy object: create only one per application
  val system = ActorSystem("mySystem")
  val readFileActor =  system.actorOf(Props[ReadFile], "readfile")
  val countActor = system.actorOf(Props[CounterActor], "count")
  val controllerActor = system.actorOf(Props[Controller], "controller")
}