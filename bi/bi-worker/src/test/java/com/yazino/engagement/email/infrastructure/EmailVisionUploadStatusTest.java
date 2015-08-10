package com.yazino.engagement.email.infrastructure;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class EmailVisionUploadStatusTest {

    @Test
    public void getStatusReturnsStorageForResponseStorage(){
        EmailVisionUploadStatus actual = EmailVisionUploadStatus.getStatus("STORAGE");
        Assert.assertThat(actual, CoreMatchers.is(IsEqual.equalTo(EmailVisionUploadStatus.STORAGE)));
    }

    @Test
    public void getStatusReturnsValidatedForResponseValidated(){
        EmailVisionUploadStatus actual = EmailVisionUploadStatus.getStatus("VALIDATED");
        Assert.assertThat(actual, CoreMatchers.is(IsEqual.equalTo(EmailVisionUploadStatus.VALIDATED)));
    }

    @Test
    public void getStatusReturnsCorrectEnumForResponseQueued(){
        EmailVisionUploadStatus actual = EmailVisionUploadStatus.getStatus("QUEUED");
        Assert.assertThat(actual, CoreMatchers.is(IsEqual.equalTo(EmailVisionUploadStatus.QUEUED)));
    }

    @Test
    public void getStatusReturnsCorrectEnumForResponseImporting(){
        EmailVisionUploadStatus actual = EmailVisionUploadStatus.getStatus("IMPORTING");
        Assert.assertThat(actual, CoreMatchers.is(IsEqual.equalTo(EmailVisionUploadStatus.IMPORTING)));
    }

    @Test
    public void getStatusReturnsCorrectEnumForResponseError(){
        EmailVisionUploadStatus actual = EmailVisionUploadStatus.getStatus("ERROR");
        Assert.assertThat(actual, CoreMatchers.is(IsEqual.equalTo(EmailVisionUploadStatus.ERROR)));
    }

    @Test
    public void getStatusReturnsCorrectEnumForResponseDoneWithError(){
        EmailVisionUploadStatus actual = EmailVisionUploadStatus.getStatus("DONE WITH ERROR(S)");
        Assert.assertThat(actual, CoreMatchers.is(IsEqual.equalTo(EmailVisionUploadStatus.DONE_WITH_ERRORS)));
    }
}
