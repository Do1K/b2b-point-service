# 1. 베이스 이미지 선택
# Java 17을 실행할 수 있는 가벼운 Temurin 이미지를 사용합니다.
FROM eclipse-temurin:17-jre-jammy

# 2. 작업 디렉토리 설정
# 컨테이너 내부에 /app이라는 폴더를 만들고 앞으로의 모든 작업을 여기서 수행합니다.
WORKDIR /app

# 3. 빌드된 JAR 파일 복사
# 빌드 과정에서 생성된 .jar 파일을 컨테이너의 /app 폴더로 복사하고 이름을 app.jar로 변경합니다.
# "build/libs/*.jar" 부분은 프로젝트의 빌드 결과물 경로에 맞게 조정될 수 있습니다.
COPY b2b-point/build/libs/*.jar app.jar

# 4. 애플리케이션 실행
# 컨테이너가 시작될 때 실행할 명령어를 정의합니다.
# 운영 환경 프로파일(prod)을 활성화하고, 프로세스 이름을 지정하여 실행합니다.
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "-Duser.name=b2b-point", "app.jar"]