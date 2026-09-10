resource "aws_cloudwatch_log_group" "smartfinancetracker_ec2" {
  name = "/smartfinancetracker/ec2"

  retention_in_days = 7
}