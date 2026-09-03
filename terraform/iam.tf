resource "aws_iam_openid_connect_provider" "github_actions" {
  url = "https://token.actions.githubusercontent.com"

  client_id_list = [
    "sts.amazonaws.com"
  ]

  thumbprint_list = ["ab9d0263244dd0326eb67015705a667e79cfe998"]
}

resource "aws_iam_role" "github_actions_deploy" {
  name = "github-actions-deploy-role"

  assume_role_policy = <<EOF
   {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Effect": "Allow",
              "Principal": {
                  "Federated": "arn:aws:iam::310697202929:oidc-provider/token.actions.githubusercontent.com"
              },
              "Action": "sts:AssumeRoleWithWebIdentity",
              "Condition": {
                  "StringEquals": {
                      "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
                  },
                  "StringLike": {
                      "token.actions.githubusercontent.com:sub": [
                          "repo:taitran5420/SmartFinanceTracker:ref:refs/heads/main",
                          "repo:taitran5420/SmartFinanceTrackerFrontEnd:ref:refs/heads/main"
                      ]
                  }
              }
          }
      ]
  }
    EOF
}

resource "aws_iam_role_policy" "github_actions_deploy" {
  name = "ecr-push-pull"
  role = aws_iam_role.github_actions_deploy.id

  policy = <<EOF
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Sid": "ECRAuth",
              "Effect": "Allow",
              "Action": "ecr:GetAuthorizationToken",
              "Resource": "*"
          },
          {
              "Sid": "ECRPushPull",
              "Effect": "Allow",
              "Action": [
                  "ecr:BatchCheckLayerAvailability",
                  "ecr:GetDownloadUrlForLayer",
                  "ecr:BatchGetImage",
                  "ecr:PutImage",
                  "ecr:InitiateLayerUpload",
                  "ecr:UploadLayerPart",
                  "ecr:CompleteLayerUpload"
              ],
              "Resource": [
                  "arn:aws:ecr:ap-southeast-1:310697202929:repository/smartfinancetracker-backend",
                  "arn:aws:ecr:ap-southeast-1:310697202929:repository/smartfinancetracker-frontend"
              ]
          }
      ]
  }
  EOF
}

resource "aws_iam_user" "taitran-local-cli" {
  name = "taitran-local-cli"

  tags = {
    (var.local_cli_access_key_id) = "access_key_cli"
  }
}

resource "aws_iam_policy" "ecr-push-local-cli" {
  name   = "ecr-push-local-cli"
  policy = <<EOF
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Sid": "ECRAuth",
              "Effect": "Allow",
              "Action": [
                  "ecr:GetAuthorizationToken",
                  "iam:ListUserPolicies",
                  "iam:ListUsers",
                  "iam:ListAttachedUserPolicies",
                  "iam:GetUserPolicy",
                  "iam:GetUser"
              ],
              "Resource": "*"
          },
          {
              "Sid": "ECRPush",
              "Effect": "Allow",
              "Action": [
                  "ecr:BatchCheckLayerAvailability",
                  "ecr:PutImage",
                  "ecr:InitiateLayerUpload",
                  "ecr:UploadLayerPart",
                  "ecr:CompleteLayerUpload",
                  "ecr:BatchGetImage",
                  "ecr:DescribeRepositories",
                  "ecr:ListTagsForResource"
              ],
              "Resource": [
                  "arn:aws:ecr:ap-southeast-1:310697202929:repository/smartfinancetracker-backend",
                  "arn:aws:ecr:ap-southeast-1:310697202929:repository/smartfinancetracker-frontend"
              ]
          },
          {
              "Effect": "Allow",
              "Action": [
                  "logs:CreateLogGroup",
                  "logs:PutRetentionPolicy"
              ],
              "Resource": "arn:aws:logs:ap-southeast-1:310697202929:log-group:/smartfinancetracker/ec2"
          },
          {
              "Effect": "Allow",
              "Action": [
                  "logs:DescribeLogGroups",
                  "logs:DescribeLogStreams",
                  "logs:GetLogEvents",
                  "logs:FilterLogEvents"
              ],
              "Resource": "arn:aws:logs:ap-southeast-1:310697202929:log-group:/smartfinancetracker/ec2:*"
          },
          {
              "Effect": "Allow",
              "Action": [
                  "iam:ListOpenIDConnectProviders",
                  "iam:GetOpenIDConnectProvider",
                  "iam:GetRole",
                  "iam:ListRolePolicies",
                  "iam:GetRolePolicy",
                  "iam:ListAttachedRolePolicies",
                  "iam:GetUser",
                  "iam:ListUserPolicies",
                  "iam:ListUsers",
                  "iam:GetUserPolicy",
                  "iam:ListGroupsForUser",
                  "iam:GetPolicy",
                  "iam:GetPolicyVersion",
                  "iam:ListInstanceProfilesForRole",
                  "iam:GetInstanceProfile",
                  "iam:CreatePolicy",
                  "iam:ListAttachedUserPolicies",
                  "iam:ListUserTags",
                  "iam:AddUserToGroup",
                  "iam:CreateUser",
                  "iam:DeleteUser",
                  "iam:RemoveUserFromGroup",
                  "iam:UpdateUser",
                  "iam:AttachUserPolicy",
                  "iam:DeleteUserPermissionsBoundary",
                  "iam:DeleteUserPolicy",
                  "iam:DetachUserPolicy",
                  "iam:PutUserPermissionsBoundary",
                  "iam:PutUserPolicy"
              ],
              "Resource": "*"
          }
      ]
  }
  EOF
}

resource "aws_iam_user_policy_attachment" "ecr-push-local-cli" {
  policy_arn = aws_iam_policy.ecr-push-local-cli.arn
  user       = aws_iam_user.taitran-local-cli.name
}

resource "aws_iam_role" "ec2-ecr-pull-role" {
  name               = "ec2-ecr-pull-role"
  description        = "Allows EC2 instances to call AWS services on your behalf."
  assume_role_policy = <<EOF
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Effect": "Allow",
              "Principal": {
                  "Service": "ec2.amazonaws.com"
              },
              "Action": "sts:AssumeRole"
          }
      ]
  }
  EOF
}

resource "aws_iam_role_policy" "ec2-ecr-pull-rolePolicy" {
  role = aws_iam_role.ec2-ecr-pull-role.id
  name = "ec2-ecr-pull-rolePolicy"

  policy = <<EOF
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Effect": "Allow",
              "Action": "ecr:GetAuthorizationToken",
              "Resource": "*"
          },
          {
              "Effect": "Allow",
              "Action": [
                  "ecr:BatchGetImage",
                  "ecr:GetDownloadUrlForLayer",
                  "ecr:BatchCheckLayerAvailability"
              ],
              "Resource": [
                  "arn:aws:ecr:ap-southeast-1:310697202929:repository/smartfinancetracker-backend",
                  "arn:aws:ecr:ap-southeast-1:310697202929:repository/smartfinancetracker-frontend"
              ]
          },
          {
              "Effect": "Allow",
              "Action": [
                  "logs:CreateLogGroup",
                  "logs:CreateLogStream",
                  "logs:PutLogEvents"
              ],
              "Resource": "arn:aws:logs:ap-southeast-1:310697202929:log-group:/smartfinancetracker/ec2:*"
          },
          {
              "Effect": "Allow",
              "Action": "logs:DescribeLogGroups",
              "Resource": "*"
          },
          {
              "Effect": "Allow",
              "Action": [
                  "logs:GetLogGroupFields",
                  "logs:StartQuery",
                  "logs:StopQuery",
                  "logs:GetQueryResults",
                  "logs:GetLogEvents"
              ],
              "Resource": "arn:aws:logs:ap-southeast-1:310697202929:log-group:/smartfinancetracker/ec2:*"
          },
          {
              "Effect": "Allow",
              "Action": [
                  "logs:DescribeLogGroups",
                  "cloudwatch:ListMetrics"
              ],
              "Resource": "*"
          }
      ]
  }
  EOF
}

resource "aws_iam_role_policy" "cloud_watch_logs_policy" {
  name = "cloud_watch_logs_policy"
  role = aws_iam_role.ec2-ecr-pull-role.id

  policy = <<EOF
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Sid": "AllowGrafanaCloudWatchLogs",
              "Effect": "Allow",
              "Action": [
                  "logs:DescribeLogGroups",
                  "logs:GetLogEvents",
                  "logs:FilterLogEvents",
                  "logs:StartQuery",
                  "logs:StopQuery",
                  "logs:GetQueryResults",
                  "logs:GetLogRecord",
                  "logs:ListAggregateLogGroupSummaries"
              ],
              "Resource": "*"
          },
          {
              "Sid": "AllowGrafanaOAM",
              "Effect": "Allow",
              "Action": "oam:ListSinks",
              "Resource": "*"
          },
          {
              "Sid": "AllowGrafanaCloudWatchMetrics",
              "Effect": "Allow",
              "Action": [
                  "cloudwatch:ListMetrics",
                  "cloudwatch:GetMetricData"
              ],
              "Resource": "*"
          }
      ]
  }
  EOF
}

resource "aws_iam_instance_profile" "ec2-ecr-pull-role" {
  name = "ec2-ecr-pull-role"
  role = aws_iam_role.ec2-ecr-pull-role.id
}

resource "aws_iam_policy" "ec2-readonly-local-cli" {
  name   = "ec2-readonly-local-cli"
  policy = <<EOF
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Sid": "EC2ReadOnly",
              "Effect": "Allow",
              "Action": [
                  "ec2:DescribeInstances",
                  "ec2:DescribeSecurityGroups",
                  "ec2:DescribeKeyPairs",
                  "ec2:DescribeAddresses",
                  "ec2:DescribeSubnets",
                  "ec2:DescribeVpcs"
              ],
              "Resource": "*"
          }
      ]
  }
  EOF
}

resource "aws_iam_user_policy_attachment" "ec2-readonly-local-cli" {
  policy_arn = aws_iam_policy.ec2-readonly-local-cli.arn
  user       = aws_iam_user.taitran-local-cli.name
}