# hello-aws-firehose-s3-athena: 실시간 로그 수집 및 분석 시스템

## 🚀 개요
이 프로젝트는 AWS Kinesis Firehose를 통해 실시간으로 로그 데이터를 수집하고, S3에 저장된 데이터를 AWS Glue 및 Athena를 활용하여 효율적으로 분석하는 시스템입니다. 또한, 분석된 데이터를 기반으로 Excel 보고서를 생성하는 기능을 포함합니다.

## ✨ 주요 기능
*   **실시간 로그 수집**: AWS Kinesis Firehose를 통해 애플리케이션 로그를 S3로 스트리밍합니다.
*   **모의 로그 생성**: Spring Scheduler를 활용하여 테스트 및 데모를 위한 모의 사용자 활동 로그를 주기적으로 생성합니다.
*   **데이터 레이크 구축**: S3에 저장된 로그 데이터를 AWS Glue Catalog에 등록하여 Athena를 통한 쿼리 준비를 완료합니다.
*   **고성능 로그 분석**: AWS Athena를 사용하여 S3에 저장된 대규모 로그 데이터를 빠르고 효율적으로 쿼리합니다.
*   **이력 조회 API**: 특정 계정의 활동 이력을 조회하는 REST API를 제공합니다.
*   **Excel 보고서 생성**: 개인(신용)정보 이용/제공 내역과 같은 맞춤형 Excel 보고서를 비밀번호 보호 기능과 함께 생성합니다.

## 🏛️ 아키텍처
```
[Spring Boot Application]
    ├── MockLogGenerator (로그 생성)
    ├── LogScheduler (주기적 로그 전송)
    └── LogProducer (Firehose로 로그 전송)
            ↓
[AWS Kinesis Firehose]
            ↓
[AWS S3 (로그 저장)]
            ↓
[AWS Glue Catalog] (데이터 스키마 및 파티션 관리)
            ↓
[AWS Athena] (S3 데이터 쿼리)
            ↑
[Spring Boot Application]
    └── HistoryController (Athena 쿼리 API)
    └── ExcelGenerator (Excel 보고서 생성)
```
이 시스템은 Spring Boot 애플리케이션에서 모의 로그를 생성하여 Kinesis Firehose로 전송합니다. Firehose는 이 로그를 S3 버킷에 저장하며, AWS Glue Catalog는 S3에 저장된 데이터의 스키마와 파티션 정보를 관리합니다. AWS Athena는 Glue Catalog를 기반으로 S3의 데이터를 쿼리하며, Spring Boot 애플리케이션은 이 Athena 쿼리 결과를 REST API를 통해 제공하거나 Excel 보고서 생성에 활용합니다.

## 🛠️ 기술 스택
*   **Backend**: Kotlin, Spring Boot 3.x, Gradle
*   **AWS Services**: Kinesis Firehose, S3, Glue, Athena, IAM, CloudWatch
*   **Infrastructure as Code**: Terraform
*   **Data Processing**: Apache POI (for Excel generation)
*   **Serialization**: Jackson

## ⚙️ 설정 및 배포

### AWS 인프라 (Terraform)
Terraform을 사용하여 필요한 AWS 리소스(S3 버킷, Firehose Delivery Stream, Glue Database/Table, Athena Workgroup 등)를 배포합니다.
1.  AWS CLI가 구성되어 있고 Terraform이 설치되어 있는지 확인합니다.
2.  프로젝트 루트 디렉토리에서 `terraform` 폴더로 이동합니다.
    ```bash
    cd terraform
    ```
3.  Terraform을 초기화합니다.
    ```bash
    terraform init
    ```
4.  배포 계획을 확인합니다.
    ```bash
    terraform plan
    ```
5.  리소스를 배포합니다.
    ```bash
    terraform apply
    ```
    배포가 완료되면, `terraform output` 명령을 통해 생성된 리소스의 이름(예: S3 버킷 이름, Firehose 스트림 이름)을 확인할 수 있습니다. 이 정보는 Spring Boot 애플리케이션 설정에 필요합니다.

### Spring Boot 애플리케이션
Spring Boot 애플리케이션을 로컬에서 실행하거나 배포합니다.
1.  프로젝트 루트 디렉토리로 이동합니다.
    ```bash
    cd /path/to/your/project/hello-aws-firehose-s3-athena
    ```
2.  Terraform 배포 시 확인한 AWS 리소스 이름들을 `src/main/resources/application.properties` 파일에 설정합니다. **주의: AWS 자격 증명은 환경 변수로 설정해야 합니다.**
    ```properties
    # src/main/resources/application.properties 예시
    spring.application.name=excel-demo

    aws.region=ap-northeast-2
    aws.firehose.stream-name=your-firehose-stream-name # Terraform output에서 확인
    aws.athena.database=your-athena-database-name # Terraform output에서 확인
    aws.athena.workgroup=your-athena-workgroup-name # Terraform output에서 확인
    aws.athena.results-bucket=s3://your-athena-results-bucket # Terraform output에서 확인

    # AWS 자격 증명은 환경 변수로 설정 (예: export AWS_ACCESS_KEY_ID=..., export AWS_SECRET_ACCESS_KEY=...)
    aws.credentials.access-key=${AWS_ACCESS_KEY_ID:}
    aws.credentials.secret-key=${AWS_SECRET_ACCESS_KEY:}

    # 스케줄러 활성화 여부 (모의 로그 생성)
    scheduler.enabled=true
    scheduler.max-records=1000000
    ```
3.  애플리케이션을 실행합니다.
    ```bash
    ./gradlew bootRun
    ```
    또는 JAR 파일을 빌드하여 실행할 수 있습니다.
    ```bash
    ./gradlew build
    java -jar build/libs/excel-demo-0.0.1-SNAPSHOT.jar
    ```

## 🌐 API 엔드포인트

*   **GET /history/{accountId}**: 특정 `accountId`의 활동 이력을 조회합니다.
    *   **Path Variables**: `accountId` (Long)
    *   **Request Params**: `period` (String, 필수, 예: `3m`, `6m`, `12m`, `36m`), `cursor` (Long, 선택), `limit` (Int, 선택, 기본값 5)
    *   **Example Request**: `GET /history/12345?period=3m&limit=10`
    *   **Response**: `HistoryResponse` 객체 (데이터, 다음 커서, 다음 페이지 존재 여부 포함)

## 📊 Athena 쿼리 최적화
이 프로젝트의 Athena 쿼리는 다음과 같은 최적화 기법을 활용하여 고성능 및 비용 효율성을 달성합니다.
*   **일별 파티셔닝 (`dt`)**: 로그 데이터는 날짜(`dt`)를 기준으로 일별 파티셔닝되어 있습니다.
*   **파티션 프로젝션(Partition Projection)**: AWS Glue 테이블에 파티션 프로젝션이 활성화되어 있어, Athena가 쿼리 시 `dt` 필터를 기반으로 필요한 파티션만 자동으로 스캔합니다. 이는 불필요한 데이터 스캔을 방지하고 쿼리 속도를 크게 향상시킵니다.
*   **Parquet 형식 및 Snappy 압축**: 데이터는 컬럼 기반의 Parquet 형식으로 저장되며 Snappy 압축이 적용되어, 스캔되는 데이터 양을 최소화하고 I/O 성능을 최적화합니다.
*   **효과적인 `WHERE` 절 필터링**: 쿼리에서 `dt BETWEEN ...`과 같이 파티션 키를 명시적으로 필터링하여 파티션 프루닝을 극대화합니다.

이러한 조합을 통해 Athena는 최소한의 데이터만 스캔하여 빠르고 비용 효율적인 분석을 수행합니다.
