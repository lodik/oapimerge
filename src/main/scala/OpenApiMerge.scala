import java.nio.file.{Files, Path, Paths}

import cats.implicits._
import io.circe.Json
import io.circe.syntax.EncoderOps
import io.circe.yaml.parser

object OpenApiMerge extends App {
  val eprintln: String => Unit = System.err.println
  val clArgs = CommandLineArgs.parse(args)

  def updateKeys(key: String, f: Json => Json)(json: Json): Json =
    json match {
      case json if json.isObject =>
        json.asObject.get.toMap.collect {
          case (k, v) if k == key => (k, f(v))
          case (k, v)             => (k, updateKeys(key, f)(v))
        }.asJson
      case json if json.isArray =>
        json.asArray.get.map(updateKeys(key, f)).asJson
      case json: Json => json
    }

  def replaceRef(refNode: String, currentSchemaLocation: Path): String = {
    val currentDir = currentSchemaLocation.getParent.toAbsolutePath
    val targetDir = clArgs.output.toAbsolutePath.getParent.toAbsolutePath

    (if (refNode.startsWith("#")) {
       targetDir.relativize(Paths.get(currentSchemaLocation.toAbsolutePath.toString)).toString + refNode
     } else {
       if (refNode.indexOf("#") > -1) {
         val prefRelPath = refNode.substring(0, refNode.indexOf("#"))
         targetDir.relativize(currentDir.resolve(prefRelPath).toAbsolutePath).toString + refNode.substring(
           refNode.indexOf("#"))
       } else {
         targetDir.relativize(currentDir.resolve(refNode).toAbsolutePath).toString
       }
     })
      .replace("\\", "/")
  }

  val targetSchema = clArgs.schemas
    .map(f => (f, parser.parse(Files.newBufferedReader(f))))
    .map(t => (t._1, t._2.toOption.get))
    .foldLeft(Json.obj("openapi" -> "3.0.0".asJson)) {
      case (res, (schemaLocation, schema)) =>
        val paths = schema.asObject.get("paths").get
        val pathsWithUpdatedRefs = updateKeys(
          "$ref",
          refNode => replaceRef(refNode.asString.get, schemaLocation).asJson
        )(paths)

        schema.asObject.flatMap(_("components")).flatMap(_.asObject).flatMap(_("securitySchemes")).flatMap(_.asObject)

        val targetInfo = res.asObject
          .flatMap(_("info"))
          .flatMap(_.asObject)
          .map(_.filterKeys("description" =!= _))
          .flatMap(
            sourceInfo =>
              schema.asObject
                .flatMap(_("info"))
                .flatMap(_.asObject)
                .map(_.filterKeys("description" =!= _))
                .map(sourceInfo.deepMerge))
          .map(_.asJson)
          .getOrElse(Json.obj())
        val targetPaths = res.asObject.flatMap(_("paths")).getOrElse(Json.obj()).deepMerge(pathsWithUpdatedRefs)
        val targetComponents = res.asObject
          .flatMap(_("components"))
          .getOrElse(Json.obj())
          .deepMerge(
            Json
              .obj(
                "securitySchemes" -> schema.asObject
                  .flatMap(_("components"))
                  .flatMap(_.asObject)
                  .flatMap(_("securitySchemes"))
                  .getOrElse(Json.obj())
                  .deepMerge(
                    res.asObject
                      .flatMap(_("components"))
                      .getOrElse(Json.obj())
                      .asObject
                      .flatMap(_("securitySchemes"))
                      .getOrElse(Json.obj()))))
        val resultObject = res.asObject.get
        resultObject
          .add("info", targetInfo)
          .add("paths", targetPaths)
          .add("components", targetComponents)
          .asJson
    }

  val filename = clArgs.output.getFileName.toString

  val content = filename.substring(filename.lastIndexOf(".") + 1) match {
    case "yaml" | "yml" => io.circe.yaml.printer.print(targetSchema).getBytes
    case "json" => targetSchema.toString().getBytes
    case format =>
      eprintln(s"Unsupported output format: $format. Use one of: yaml, json")
      sys.exit(1)
  }
  Files.write(clArgs.output, content)
}
