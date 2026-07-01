FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon || true
COPY src/ src/
COPY config/ config/
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
# JasperReports の PDF 生成は AWT のフォント初期化を通る。これが無いと
# 「Fontconfig head is null」「No physical fonts found」で PDF エクスポートが 500 になる:
#   - fontconfig        … 正常な /etc/fonts を用意し head is null を回避
#   - fonts-dejavu-core … AWT 初期化用の物理フォント（No physical fonts found を回避）
# ※ レポート本文の日本語は jar 内の Font Extension（Noto Sans JP）を PDF 埋め込みするため、
#    日本語フォントを OS に入れる必要はない。ここで入れるのは AWT 初期化を通すためだけ。
RUN apt-get update \
	&& apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
	&& rm -rf /var/lib/apt/lists/* \
	&& fc-cache -f
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]
