option explicit

Main( )

sub Main( )
    dim objArgs, con, endis, con2

    Set objArgs = WScript.Arguments

    if objArgs.Count > 0 then
        con = objArgs(0)
        con2 = objArgs(1)
        endis = objArgs(2)
        
        EnableDisableICS con, con2, endis  'true enables, false disables
        
    else
        DIM szMsg
        szMsg = "Invalid usage! Please provide the name of the connection as the argument." & vbCRLF & vbCRLF & _
                "Usage:" & vbCRLF & _ 
                "       " + WScript.scriptname + " ""Public Connection Name"" ""Private Connection Name"" true/false"
        WScript.Echo( szMsg )  
                      
    end if

end sub

function EnableDisableICS(sPublicConnectionName, sPrivateConnectionName, bEnable)
    
    dim  bFound
    bFound = FALSE
    dim oNetSharingManager, oConnectionCollection, oItem, EveryConnection, objNCProps
    
    set oNetSharingManager = Wscript.CreateObject("HNetCfg.HNetShare.1")
     if (IsObject(oNetSharingManager)) = FALSE then
'         Wscript.Echo("Unable to get the HNetCfg.HnetShare.1 object.")
        Exit function
    End if
    
    if (IsNull(oNetSharingManager.SharingInstalled) = TRUE) then
'         Wscript.Echo( "Sharing is not available on this platform.")
        Exit function
    End if

    set oConnectionCollection = oNetSharingManager.EnumEveryConnection
    for each oItem In oConnectionCollection
        set EveryConnection = oNetSharingManager.INetSharingConfigurationForINetConnection (oItem)
        set objNCProps = oNetSharingManager.NetConnectionProps (oItem)
         If  objNCProps.guid = sPrivateConnectionName Then
            bFound = True
            If bEnable Then
                EveryConnection.EnableSharing(1)
            Else
                EveryConnection.DisableSharing
            End if
        End if
    Next

    set oConnectionCollection = oNetSharingManager.EnumEveryConnection
    for each oItem In oConnectionCollection
        set EveryConnection = oNetSharingManager.INetSharingConfigurationForINetConnection (oItem)
        set objNCProps = oNetSharingManager.NetConnectionProps (oItem)
        
        If  objNCProps.guid = sPublicConnectionName Then
            bFound = True
            If bEnable Then
                EveryConnection.EnableSharing(0)
            Else
                EveryConnection.DisableSharing
            End if
        End if
    next

'     If Not bFound Then
'        Wscript.Echo("Unable to find the connection " & sPublicConnectionName)
'     End if

end function